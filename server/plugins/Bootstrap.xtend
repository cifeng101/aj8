import java.io.File
import java.lang.reflect.Modifier
import mobs.InitialMobSpawns
import org.apollo.game.command.CommandDispatcher
import org.apollo.game.command.CommandListener
import org.apollo.game.event.EventSubscriber
import org.apollo.game.interact.ItemActionEvent
import org.apollo.game.interact.ObjectActionEvent
import org.apollo.game.model.World
import org.apollo.game.interact.ButtonActionEvent

class Bootstrap {

	def initObjects(World world) {
		classes('objects').forEach[world.provideSubscriber(it.newInstance as EventSubscriber<ObjectActionEvent>)]
	}

	def initButtons(World world) {
		classes('buttons').forEach[world.provideSubscriber(it.newInstance as EventSubscriber<ButtonActionEvent>)]
	}

	def initItems(World world) {
		classes('items').forEach[world.provideSubscriber(it.newInstance as EventSubscriber<ItemActionEvent>)]
	}

	def initSpawns(World world) {
		new InitialMobSpawns(world).init
	}

	def initCommands() {
		classes('commands').forEach[CommandDispatcher.getInstance.bind(it.newInstance as CommandListener)]
	}

	def classes(String dir) {
		val files = new File('bin/' + dir, '/').list
		val classes = newArrayList
		val filtered = files?.filter[it.endsWith('.class') && !it.contains('$')]

		filtered.forEach [
			val name = it.substring(0, it.indexOf('.'))
			val clazz = Class.forName(dir + '.' + name)
			if (!Modifier.isAbstract(clazz.modifiers) && !Modifier.isInterface(clazz.modifiers)) {
				classes += clazz
			}
		]

		return classes
	}

	new(World world) {
		world.initButtons
		world.initSpawns
		world.initItems
		world.initObjects

		initCommands
	}

}
