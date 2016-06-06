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

import java.util.ArrayList;

public class NewsHelper {

    public static int nbrOfNews;
    public static ArrayList<String> doneNodes = new ArrayList<String>();

    public static int getNbrOfNews() {
        return nbrOfNews;
    }

    public static void increaseNbrOfNews() {
        nbrOfNews++;
    }

    public static ArrayList<String> getDoneNodes() {
        return doneNodes;
    }

    public static void addDoneNodes(String doneNodesId) {
        if (!doneNodes.contains(doneNodesId)) {
            doneNodes.add(doneNodesId);
        }
    }
}