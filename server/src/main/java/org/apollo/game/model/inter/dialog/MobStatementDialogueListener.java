package org.apollo.game.model.inter.dialog;

import static org.apollo.game.model.inter.dialog.DialogueConstants.MOB_DIALOGUE_ID;

import org.apollo.game.model.Player;
import org.apollo.game.model.def.MobDefinition;
import org.apollo.game.msg.impl.InterfaceModelAnimationMessage;
import org.apollo.game.msg.impl.MobModelOnInterfaceMessage;
import org.apollo.game.msg.impl.SetInterfaceTextMessage;

/**
 * An abstract implementation of a {@link DialogueListener} which manages
 * dialogue statements that show a mob model.
 *
 * @author Ryley Kimmel <ryley.kimmel@live.com>
 */
public abstract class MobStatementDialogueListener implements DialogueListener {

	/**
	 * The identifier of the mob speaking the dialogue.
	 */
	private final int mobId;

	/**
	 * Constructs a new {@link MobStatementDialogueListener}.
	 *
	 * @param mobId The mobs identifier.
	 */
	public MobStatementDialogueListener(int mobId) {
		this.mobId = mobId;
	}

	@Override
	public final int send(Player player) {
		String[] lines = getLines();
		int length = lines.length;

		int dialogueId = MOB_DIALOGUE_ID[length - 1];
		int headChildId = dialogueId - 2;

		player.send(new MobModelOnInterfaceMessage(mobId, headChildId));
		player.send(new InterfaceModelAnimationMessage(expression().getAnimation(), headChildId));
		player.send(new SetInterfaceTextMessage(dialogueId - 1, MobDefinition.forId(mobId).getName()));

		for (int index = 0; index < length; index++) {
			player.send(new SetInterfaceTextMessage(MOB_DIALOGUE_ID[length - 1] + index, lines[index]));
		}

		return dialogueId -= 3;
	}

	@Override
	public final int getMaximumEntries() {
		return MOB_DIALOGUE_ID.length;
	}

	/* Do not allow method overriding for these methods. */
	@Override
	public final void optionClicked(DialogueOption option) {

	}

}