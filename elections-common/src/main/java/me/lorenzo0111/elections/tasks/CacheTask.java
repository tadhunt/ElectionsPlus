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
import java.util.concurrent.atomic.AtomicInteger;

import me.lorenzo0111.elections.cache.CacheManager;
import me.lorenzo0111.elections.database.IDatabaseManager;

public class CacheTask implements Runnable {
    private final IDatabaseManager database;
    private final CacheManager cache;

    public CacheTask(IDatabaseManager database, CacheManager cache) {
        this.database = database;
        this.cache = cache;
    }

    @Override
    public void run() {
        AtomicInteger completions = new AtomicInteger(0);
        CompletableFuture<Boolean> reloaded = new CompletableFuture<Boolean>();

        database.getParties()
                .thenAccept((parties) -> {
                    cache.getParties().reset();
                    parties.forEach(party -> cache.getParties().add(party.getName(), party));
                    if(completions.addAndGet(1) == 3) {
                        reloaded.complete(true);
                    }
                });

        database.getElections()
                .thenAccept((elections) -> {
                    cache.getElections().reset();
                    elections.forEach(election -> cache.getElections().add(election.getName(), election));
                    if(completions.addAndGet(1) == 3) {
                        reloaded.complete(true);
                    }
                });

        database.getVotes()
                .thenAccept((votes) -> {
                    cache.getVotes().reset();
                    votes.forEach(vote -> cache.getVotes().add(vote.getVoteId().toString(), vote));
                    if(completions.addAndGet(1) == 3) {
                        reloaded.complete(true);
                    }
                });

        reloaded.thenAccept((result) -> {
            cache.getEventHandler().onCacheReloaded();
        });
    }
}
