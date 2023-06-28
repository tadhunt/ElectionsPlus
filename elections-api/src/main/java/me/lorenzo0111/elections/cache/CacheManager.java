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

package me.lorenzo0111.elections.cache;

import java.util.UUID;

import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.CacheEventHandler;
import me.lorenzo0111.elections.api.objects.EClaim;
import me.lorenzo0111.elections.api.objects.Election;
import me.lorenzo0111.elections.api.objects.Party;
import me.lorenzo0111.elections.api.objects.Vote;

public class CacheManager {
    private final CacheEventHandler eventHandler;
    private final Cache<UUID, Party> parties;
    private final Cache<UUID, Election> elections;
    private final Cache<UUID, Vote> votes;
    private final Cache<UUID, EClaim> claims;

    public CacheManager(CacheEventHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.parties = new MapCache<>();
        this.elections = new MapCache<>();
        this.votes = new MapCache<>();
        this.claims = new MapCache<>();
    }

    public Cache<UUID, Party> getParties() {
        return parties;
    }

    public Cache<UUID, Election> getElections() {
        return elections;
    }

    public Cache<UUID, Vote> getVotes() {
        return votes;
    }

    public Cache<UUID, EClaim> getClaims() {
        return claims;
    }

    public CacheEventHandler getEventHandler() {
        return eventHandler;
    }

    public void persist() {
        parties.persist();
        elections.persist();
        votes.persist();
        claims.persist();
    }
}
