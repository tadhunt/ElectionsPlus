/*
 * This file is part of ElectionsPlus, licensed under the MIT License.
 *
 * Copyright (c) Lorenzo0111
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.lorenzo0111.elections.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import me.lorenzo0111.elections.cache.CacheManager;
import me.lorenzo0111.elections.database.IDatabaseManager;
import me.lorenzo0111.elections.scheduler.IAdvancedScheduler;

public class CacheTask implements Runnable {
    private Logger logger;
    private final IAdvancedScheduler scheduler;
    private final IDatabaseManager database;
    private final CacheManager cache;

    public CacheTask(Logger logger, IAdvancedScheduler scheduler, IDatabaseManager database, CacheManager cache) {
        this.logger = logger;
        this.scheduler = scheduler;
        this.database = database;
        this.cache = cache;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        int nCompletions = 6;
        AtomicInteger completions = new AtomicInteger(0);
        CompletableFuture<Boolean> reloaded = new CompletableFuture<Boolean>();

        database.getVotes()
                .thenAccept((votes) -> {
                    cache.getVotes().reset();
                    votes.forEach(vote -> cache.getVotes().add(vote.getId(), vote));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        database.getParties()
                .thenAccept((parties) -> {
                    cache.getParties().reset();
                    parties.values().forEach(party -> cache.getParties().add(party.getId(), party));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        database.getElections()
                .thenAccept((elections) -> {
                    cache.getElections().reset();
                    elections.values().forEach(election -> cache.getElections().add(election.getId(), election));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        database.getBlocks()
                .thenAccept((blocks) -> {
                    cache.getBlocks().reset();
                    blocks.forEach(block -> cache.getBlocks().add(block.getId(), block));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        database.getHolograms()
                .thenAccept((holograms) -> {
                    cache.getHolograms().reset();
                    holograms.values().forEach(hologram -> cache.getHolograms().add(hologram.getId(), hologram));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        database.getClaims()
                .thenAccept((claims) -> {
                    cache.getClaims().reset();
                    claims.values().forEach(claim -> cache.getClaims().add(claim.getId(), claim));
                    if(completions.addAndGet(1) == nCompletions) {
                        reloaded.complete(true);
                    }
                });

        reloaded.thenAccept((result) -> {
            long elapsedMs = System.currentTimeMillis() - start;

            cache.getEventHandler().onCacheReloaded();

            scheduler.repeating(this.persist(), 60 * 20L, 60, TimeUnit.SECONDS);
            logger.info(String.format("Loaded in %d ms.", elapsedMs));
        });
    }

    private Runnable persist() {
        return () -> {
            int nMutations = cache.persist();
            if (nMutations > 0) {
                logger.warning(String.format("CacheTask: persisted %d changes", nMutations));
            }
        };
    }
}
