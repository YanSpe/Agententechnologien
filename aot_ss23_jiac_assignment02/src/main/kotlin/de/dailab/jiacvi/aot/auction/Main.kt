package de.dailab.jiacvi.aot.auction

import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem
import de.dailab.jiacvi.aot.auction.myBidder.DummyBidderAgent
import de.dailab.jiacvi.aot.auction.myBidder.DummyBidderAgent01


fun main() {
    agentSystem("Auction") {
        enable(LocalBroker)
        // TODO change the amount of bidders here or the composition if you have different types of agents for testing.
        agents {
            add(AuctioneerAgent(3,
                5, 1))
            for (i in (1..2)) {
                add(DummyBidderAgent("dummy-$i"))
            }
            add(DummyBidderAgent01("dummy-3"))
        }
    }.start()
}
