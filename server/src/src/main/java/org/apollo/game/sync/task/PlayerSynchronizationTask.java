package org.apollo.game.sync.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apollo.game.model.Player;
import org.apollo.game.model.Position;
import org.apollo.game.model.World;
import org.apollo.game.msg.impl.PlayerSynchronizationMessage;
import org.apollo.game.sync.block.AppearanceBlock;
import org.apollo.game.sync.block.ChatBlock;
import org.apollo.game.sync.block.SynchronizationBlock;
import org.apollo.game.sync.block.SynchronizationBlockSet;
import org.apollo.game.sync.seg.AddCharacterSegment;
import org.apollo.game.sync.seg.MovementSegment;
import org.apollo.game.sync.seg.RemoveCharacterSegment;
import org.apollo.game.sync.seg.SynchronizationSegment;
import org.apollo.game.sync.seg.TeleportSegment;

/**
 * A {@link SynchronizationTask} which synchronizes the specified {@link Player}
 * .
 *
 * @author Graham
 */
public final class PlayerSynchronizationTask extends SynchronizationTask {

	/**
	 * The maximum number of players to load per cycle. This prevents the update
	 * packet from becoming too large (the client uses a 5000 byte buffer) and
	 * also stops old spec PCs from crashing when they login or teleport.
	 */
	private static final int NEW_PLAYERS_PER_CYCLE = 20;

	/**
	 * The player.
	 */
	private final Player player;

	/**
	 * The world.
	 */
	private final World world;

	/**
	 * Creates the {@link PlayerSynchronizationTask} for the specified player.
	 *
	 * @param player The player.
	 * @param world The world.
	 */
	public PlayerSynchronizationTask(Player player, World world) {
		this.player = player;
		this.world = world;
	}

	@Override
	public void run() {
		Position lastKnownRegion = player.getLastKnownRegion();
		boolean regionChanged = player.hasRegionChanged();

		SynchronizationBlockSet blockSet = player.getBlockSet();
		if (blockSet.contains(ChatBlock.class)) {
			blockSet = blockSet.clone();
			blockSet.remove(ChatBlock.class);
		}

		SynchronizationSegment segment;

		if (player.getAttributes().isTeleporting() || player.hasRegionChanged()) {
			segment = new TeleportSegment(blockSet, player.getPosition());
		} else {
			segment = new MovementSegment(blockSet, player.getDirections());
		}

		Set<Player> localPlayers = player.getLocalPlayers();
		List<SynchronizationSegment> segments = new ArrayList<>();
		int oldLocalPlayers = localPlayers.size();

		Iterator<Player> it = localPlayers.iterator();
		while (it.hasNext()) {
			Player p = it.next();
			if (!p.isActive() || p.getAttributes().isTeleporting() || p.getPosition().getLongestDelta(player.getPosition()) > player.getViewingDistance()) {
				it.remove();
				segments.add(new RemoveCharacterSegment());
			} else {
				segments.add(new MovementSegment(p.getBlockSet(), p.getDirections()));
			}
		}

		int added = 0;

		for (Player p : world.getPlayerRepository()) {
			if (localPlayers.size() >= 255) {
				player.flagExcessivePlayers();
				break;
			} else if (added >= NEW_PLAYERS_PER_CYCLE) {
				break;
			}

			if (p != player && p.getPosition().isWithinDistance(player.getPosition(), player.getViewingDistance()) && !localPlayers.contains(p)) {
				localPlayers.add(p);
				added++;

				blockSet = p.getBlockSet();
				if (!blockSet.contains(AppearanceBlock.class)) {
					// TODO check if client has cached appearance
					blockSet = blockSet.clone();
					blockSet.add(SynchronizationBlock.createAppearanceBlock(p));
				}

				segments.add(new AddCharacterSegment(blockSet, p, p.getIndex(), -1, p.getPosition()));
			}
		}

		player.send(new PlayerSynchronizationMessage(lastKnownRegion, player.getPosition(), regionChanged, segment, oldLocalPlayers, segments));
	}

}