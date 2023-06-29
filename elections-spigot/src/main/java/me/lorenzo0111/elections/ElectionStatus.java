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
package me.lorenzo0111.elections;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.lorenzo0111.elections.api.objects.Election;

public class ElectionStatus {
    private Election election;
    private Integer totalVotes;
    private HashMap<UUID, Integer> partyVotes;

    ElectionStatus(Election election) {
        this.election = election;
        this.totalVotes = 0;
        this.partyVotes = new HashMap<UUID, Integer>();
        for (UUID partyId : election.getParties().keySet()) {
            this.partyVotes.put(partyId, 0);
        }
    }

    @Override
    public String toString() {
        return String.format("election %s totalVotes %s partyVotes %s", election.getName(), totalVotes, partyVotes.toString());
    }

    public Election getElection() {
        return election;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void addVote(UUID partyId) {
        Integer partyCount = partyVotes.get(partyId);
        if (partyCount == null) {
            partyCount = 0;
        }

        partyCount++;
        partyVotes.put(partyId, partyCount);

        totalVotes++;
    }

    public Map<UUID, Integer> getPartyVotes() {
        return partyVotes;
    }

    public Map<UUID, Integer> winners() {
        Integer maxVotes = 0;
        Map<UUID, Integer> winners = new HashMap<UUID, Integer>();

        for (UUID partyId : partyVotes.keySet()) {
            Integer n = partyVotes.get(partyId);
            if (n == 0) {
                    // no votes for this party
                    continue;
            }

            if (n < maxVotes) {
                // this party has less votes than the current leader(s)
                continue;
            }

            if (n == maxVotes) {
                // this party has the same number of votes as the current leader(s)
                winners.put(partyId, n);
                continue;
            }

            // n > maxVotes
            // we have a new leader

            maxVotes = n;

            winners.clear();
            winners.put(partyId, n);
        }

        return winners;
    }
}