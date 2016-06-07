/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.core.news.util;

import java.util.*;
import se.sics.ktoolbox.util.identifiable.Identifier;

public class NewsHelper {

    public static int totalOfNews;
    public static int roundsOfLeaderSelection;
    public static int newsOfLeaderSelection;
    public static ArrayList<String> doneNodes = new ArrayList<String>();
    public static ArrayList<String> newsCoverage = new ArrayList<String>();
    public static Map<Identifier, Integer> nodeKnowledge = new HashMap<>();//for each node (defined by id), stores how many news it has received

    public static int getTotalOfNews() {
        return totalOfNews;
    }

    public static void increaseTotalOfNews() {
        totalOfNews++;
    }

    public static ArrayList<String> getDoneNodes() {
        return doneNodes;
    }

    public static void addDoneNodes(String doneNodesId) {
        if (!doneNodes.contains(doneNodesId)) {
            doneNodes.add(doneNodesId);
        }
    }

    public static void increaseRoundsOfLeaderSelection() {
        roundsOfLeaderSelection++;
    }

    public static int getRoundsOfLeaderSelection() {
        return roundsOfLeaderSelection;
    }
    
     public static void increaseNewsOfLeaderSelection() {
        newsOfLeaderSelection++;
    }

    public static int getNewsOfLeaderSelection() {
        return newsOfLeaderSelection;
    }
}
