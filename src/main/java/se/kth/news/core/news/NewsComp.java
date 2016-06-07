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
package se.kth.news.core.news;

import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.leader.LeaderSelectPort;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.play.Ping;
import se.kth.news.play.Pong;
import se.kth.news.sim.ScenarioGen;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;
import se.kth.news.sim.ScenarioGen;
import se.kth.news.core.news.util.NewsHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NewsComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NewsComp.class);
    private static final int DEFAULT_TTL = 15;
    private static final int NETWORK_SIZE = ScenarioGen.NETWORK_SIZE;//number of nodes
    private String logPrefix = " ";

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Positive<LeaderSelectPort> leaderPort = requires(LeaderSelectPort.class);
    Negative<OverlayViewUpdatePort> viewUpdatePort = provides(OverlayViewUpdatePort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    private Identifier gradientOId;
    //*******************************INTERNAL_STATE*****************************
    private NewsView localNewsView;
    private CroupierSample<NewsView> myCroupierSample;

    public NewsComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        //LOG.info("{}initiating...", logPrefix);

        gradientOId = init.gradientOId;

        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleGradientSample, gradientPort);
        subscribe(handleLeader, leaderPort);
        subscribe(handlePing, networkPort);
        subscribe(handlePong, networkPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            //LOG.info("{}starting...", logPrefix);
            updateLocalNewsView();
        }
    };

    private void updateLocalNewsView() {
        localNewsView = new NewsView(selfAdr.getId(), 0);
        //LOG.debug("{}informing overlays of new view", logPrefix);
        trigger(new OverlayViewUpdate.Indication<>(gradientOId, false, localNewsView.copy()), viewUpdatePort);
    }

    Handler handleCroupierSample = new Handler<CroupierSample<NewsView>>() {
        @Override
        public void handle(CroupierSample<NewsView> castSample) {
            myCroupierSample = castSample;
            if (NewsHelper.newsCoverage.size() != 0) {
                System.out.println("*** " + (double) (100 * (NewsHelper.newsCoverage.size() - 1) / NETWORK_SIZE) + "%");
            }
            if (castSample.publicSample.isEmpty() || (NewsHelper.getDoneNodes().contains(selfAdr.getId().toString()))) {
                if (NewsHelper.getDoneNodes().size() == NETWORK_SIZE) {
                    System.exit(0);
                }
                return;
            }

            NewsHelper.newsCoverage.clear();
            NewsHelper.newsCoverage.add(selfAdr.getId().toString());
            NewsHelper.addDoneNodes(selfAdr.getId().toString());

            Iterator<Identifier> it = castSample.publicSample.keySet().iterator();
            while (it.hasNext()) {
                KAddress partner = castSample.publicSample.get(it.next()).getSource();
                KHeader header = new BasicHeader(selfAdr, partner, Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, new Ping(selfAdr, DEFAULT_TTL));
                trigger(msg, networkPort);
                localNewsView.increaseLocalNewsViewCount();

            }
            //System.out.println("node " + localNewsView.getNodeId() + " has sent " + localNewsView.getLocalNewsViewCount() + " news");
            NewsHelper.increaseTotalOfNews();
        }
    };

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
        }
    };

    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
        }
    };
    int sumOfNews = 0;
    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<Ping, KContentMsg<?, ?, Ping>>() {

        @Override
        public void handle(Ping content, KContentMsg<?, ?, Ping> container) {
            if (!NewsHelper.newsCoverage.contains(selfAdr.getId().toString())) {
                NewsHelper.newsCoverage.add(selfAdr.getId().toString());
            }

            NewsHelper.nodeKnowledge.putIfAbsent(selfAdr.getId(), 0);
            if (NewsHelper.nodeKnowledge.containsKey(selfAdr.getId())) {
                NewsHelper.nodeKnowledge.put(selfAdr.getId(), NewsHelper.nodeKnowledge.get(selfAdr.getId()) + 1);
            }

            Iterator iter1 = NewsHelper.nodeKnowledge.entrySet().iterator();
            int sumOfNews = 0;
            while(iter1.hasNext()){
                Map.Entry mEntry = (Map.Entry) iter1.next();
                sumOfNews += (int) mEntry.getValue();
            }
            Iterator iter2 = NewsHelper.nodeKnowledge.entrySet().iterator();
            while (iter2.hasNext()) {
                Map.Entry mEntry = (Map.Entry) iter2.next();                
                //Map.Entry mEntry = (Map.Entry) iter.next();
                System.out.println(mEntry.getKey() + " : " + mEntry.getValue());
                //sumOfNews += (int) mEntry.getValue();
                System.out.println("Node " + mEntry.getKey() + " has seen " + (100 * (double)(((int) mEntry.getValue())) / sumOfNews) + "% of news");
            }
            System.out.println("sum:" + sumOfNews);
            
            LOG.info("{}received ping from:{}", logPrefix, container.getHeader().getSource());

            if (content.getTTL() > 1) {
                content.decreaseTTL();

                Iterator<Identifier> it = myCroupierSample.publicSample.keySet().iterator();
                while (it.hasNext()) {
                    KAddress partner = myCroupierSample.publicSample.get(it.next()).getSource();
                    if (!partner.equals(container.getHeader().getSource())) {
                        KHeader header = new BasicHeader(selfAdr, partner, Transport.UDP);
                        KContentMsg msg = new BasicContentMsg(header, content);
                        trigger(msg, networkPort);
                    }
                }
            }
            //LOG.info("{}received ping from:{}", logPrefix, container.getHeader().getSource());
            //trigger(container.answer(new Pong()), networkPort);
        }
    };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, KContentMsg<?, KHeader<?>, Pong>>() {

        @Override
        public void handle(Pong content, KContentMsg<?, KHeader<?>, Pong> container) {
            //LOG.info("{}received pong from:{}", logPrefix, container.getHeader().getSource());
        }
    };

    public static class Init extends se.sics.kompics.Init<NewsComp> {

        public final KAddress selfAdr;
        public final Identifier gradientOId;

        public Init(KAddress selfAdr, Identifier gradientOId) {
            this.selfAdr = selfAdr;
            this.gradientOId = gradientOId;
        }
    }
}
