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

import me.lorenzo0111.elections.api.objects.Election;

public class ElectionStatus {
    private Election election;
    private Integer totalVotes;
    private HashMap<String, Integer> partyVotes;

    ElectionStatus(Election election) {
        this.election = election;
        this.totalVotes = 0;
        this.partyVotes = new HashMap<String, Integer>();
    }

    @Override
    public String toString() {
        return String.format("election %s totalVotes %s partyVotes %s", election.getName(), totalVotes, partyVotes.toString());
    }

    Election getElection() {
        return election;
    }

    Integer totalVotes() {
        return totalVotes;
    }

    Map<String, Integer> winners() {
        Integer maxVotes = 0;
        Map<String, Integer> winners = new HashMap<String, Integer>();

        for (String partyName : partyVotes.keySet()) {
            Integer n = partyVotes.get(partyName);
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
                winners.put(partyName, n);
                continue;
            }

            // n > maxVotes
            // we have a new leader

            maxVotes = n;

            winners.clear();
            winners.put(partyName, n);
        }

        return winners;
    }

    void addVote(String partyName) {
        Integer partyCount = partyVotes.get(partyName);
        if (partyCount == null) {
            partyCount = 0;
        }

        partyCount++;
        partyVotes.put(partyName, partyCount);

        totalVotes++;
    }
}