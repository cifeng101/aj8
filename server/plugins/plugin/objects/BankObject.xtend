package plugin.objects

import org.apollo.game.event.EventContext
import org.apollo.game.event.EventSubscriber
import org.apollo.game.event.annotate.SubscribesTo
import org.apollo.game.interact.ObjectActionEvent
import org.apollo.game.model.Interfaces.InterfaceOption
import org.apollo.game.model.Player
import org.apollo.game.model.inter.bank.BankUtils

@SubscribesTo(ObjectActionEvent)
class BankObject implements EventSubscriber<ObjectActionEvent> {

	override subscribe(EventContext context, Player player, ObjectActionEvent event) {
		BankUtils.openBank(player)
	}

	override test(ObjectActionEvent event) {
		event.id == 2213 && event.option == InterfaceOption.OPTION_ONE
	}

}
