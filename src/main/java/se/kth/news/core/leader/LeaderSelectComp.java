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
package se.kth.news.core.leader;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.news.util.NewsHelper;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.news.util.NewsViewComparator;
import se.kth.news.play.Ping;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderSelectComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderSelectComp.class);
    private String logPrefix = " ";

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Negative<LeaderSelectPort> leaderSelectPort = provides(LeaderSelectPort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private Comparator viewComparator;
    private List<Container<KAddress, NewsView>> neighbors;
    private List<Container<KAddress, NewsView>> fingers;
    private int sequenceNumber = 0;
    private NewsView selfView;
    private KAddress leaderAdr;

    public LeaderSelectComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        viewComparator = new NewsViewComparator();

        subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
//        subscribe(handleLeaderUpdate, networkPort);
        // subscribe(handleLeaderUpdatePush, networkPort);
        // subscribe(handleLeaderTimeoutPush, timerPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            //TODO
            neighbors = sample.getGradientNeighbours();
            fingers = sample.getGradientFingers();
            selfView = (NewsView) sample.selfView;

            for (Container<KAddress, NewsView> finger : fingers) {
                if (viewComparator.compare(selfView, finger.getContent()) >= 0) {
                    trigger(new LeaderUpdate(selfAdr), leaderSelectPort);
                }
                NewsHelper.increaseNewsOfLeaderSelection();
            }
            NewsHelper.increaseRoundsOfLeaderSelection();
            System.out.println("Rounds of leader selection: " + NewsHelper.getRoundsOfLeaderSelection());
            System.out.println("Nbr. of News in leader selection: " + NewsHelper.getNewsOfLeaderSelection());

            Iterator<Identifier> it = sample.getGradientNeighbours().iterator();
            while (it.hasNext()) {
                GradientContainer<NewsView> currentContainer = (GradientContainer<NewsView>) it.next();
                KHeader header = new BasicHeader(selfAdr, currentContainer.getSource(), Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, new LeaderUpdatePush(selfAdr));
                System.out.println(" msg:\nfrom(leader): "+msg.getSource()+" to neighbor"+msg.getDestination());
                trigger(msg, networkPort);
            }

            LOG.debug("{}neighbours:{}", logPrefix, sample.gradientNeighbours);
            LOG.debug("{}fingers:{}", logPrefix, sample.gradientFingers);
            LOG.debug("{}local view:{}", logPrefix, sample.selfView);
        }
    };

    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
            LOG.info("{}New leader:{}", logPrefix, event.leaderAdr.getId());
        }
    };

    Handler handleLeaderUpdate = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
            System.out.println("LEADER UPDATE");
            LOG.info("{}New leader:{}", logPrefix, event.leaderAdr.getId());
        }
    };

    ClassMatchedHandler handleLeaderUpdatePush
            = new ClassMatchedHandler<LeaderUpdatePush, KContentMsg<?, ?, LeaderUpdatePush>>() {
        @Override
        public void handle(LeaderUpdatePush content, KContentMsg<?, ?, LeaderUpdatePush> container) {
            System.out.println("leader update push");
            //setLeader(container.getContent().leaderAdr);
        }
    };

    ClassMatchedHandler handleLeaderTimeoutPush
            = new ClassMatchedHandler<LeaderTimeoutPush, KContentMsg<?, ?, LeaderTimeoutPush>>() {
        @Override
        public void handle(LeaderTimeoutPush content, KContentMsg<?, ?, LeaderTimeoutPush> container) {
            System.out.println("leader timeout push");
            //setLeader(container.getContent().leaderAdr);
        }
    };

    public static class Init extends se.sics.kompics.Init<LeaderSelectComp> {

        public final KAddress selfAdr;
        public final Comparator viewComparator;

        public Init(KAddress selfAdr, Comparator viewComparator) {
            this.selfAdr = selfAdr;
            this.viewComparator = viewComparator;
        }
    }
}
