package org.apollo.fs.parser;

import static org.apollo.game.model.obj.ObjectGroup.WALL;
import static org.apollo.game.model.obj.ObjectType.DIAGONAL_WALL;
import static org.apollo.game.model.obj.ObjectType.GENERAL_PROP;
import static org.apollo.game.model.obj.ObjectType.GROUND_PROP;
import static org.apollo.game.model.obj.ObjectType.WALKABLE_PROP;
import static org.apollo.game.model.region.Tile.FLAG_BLOCKED;
import static org.apollo.game.model.region.Tile.FLAG_BRIDGE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apollo.fs.FileSystem;
import org.apollo.game.model.Position;
import org.apollo.game.model.World;
import org.apollo.game.model.def.GameObjectDefinition;
import org.apollo.game.model.def.MapDefinition;
import org.apollo.game.model.obj.GameObject;
import org.apollo.game.model.obj.ObjectOrientation;
import org.apollo.game.model.obj.ObjectType;
import org.apollo.game.model.pf.TraversalMap;
import org.apollo.util.ByteBufferUtil;
import org.apollo.util.CompressionUtil;

/**
 * A class which parses static object definitions, which include map tiles and
 * landscapes.
 *
 * @author Ryley Kimmel <ryley.kimmel@live.com>
 */
public final class StaticObjectDefinitionParser {

	/**
	 * A set of game objects
	 */
	private final List<GameObject> gameObjects = new ArrayList<>();

	/**
	 * The world.
	 */
	private final World world;

	/**
	 * Constructs a new {@link StaticObjectDefinition} with the specified world.
	 *
	 * @param world The world.
	 */
	public StaticObjectDefinitionParser(World world) {
		this.world = world;
	}

	/**
	 * Parses the map definition files from the {@link FileSystem}.
	 *
	 * @param fs The file system.
	 * @return A {@link List} of parsed {@link GameObject}s
	 * @throws IOException If some I/O exception occurs.
	 */
	public List<GameObject> parse(FileSystem fs) throws IOException {
		Map<Integer, MapDefinition> defs = MapDefinitionParser.parse(fs);

		for (Entry<Integer, MapDefinition> entry : defs.entrySet()) {
			MapDefinition def = entry.getValue();

			int hash = def.getHash();
			int x = (hash >> 8 & 0xFF) * 64;
			int y = (hash & 0xFF) * 64;

			byte[] gameObjectData = fs.getFile(FileSystem.MAP_IDX, def.getObjectFile());
			ByteBuffer gameObjectBuffer = ByteBuffer.wrap(CompressionUtil.ungzip(gameObjectData));
			parseGameObject(gameObjectBuffer, x, y);

			byte[] terrainData = fs.getFile(FileSystem.MAP_IDX, def.getTerrainFile());
			ByteBuffer terrainBuffer = ByteBuffer.wrap(CompressionUtil.ungzip(terrainData));
			parseTerrain(terrainBuffer, x, y);
		}

		return gameObjects;
	}

	/**
	 * Parses a {@link GameObject} on the specified coordinates.
	 *
	 * @param gameObjectBuffer The uncompressed game object data buffer.
	 * @param x The x coordinate this object is on.
	 * @param y The y coordinate this object is on.
	 */
	private void parseGameObject(ByteBuffer gameObjectBuffer, int x, int y) {
		for (int deltaId, id = -1; (deltaId = ByteBufferUtil.readSmart(gameObjectBuffer)) != 0;) {
			id += deltaId;

			for (int deltaPos, hash = 0; (deltaPos = ByteBufferUtil.readSmart(gameObjectBuffer)) != 0;) {
				hash += deltaPos - 1;

				int localX = hash >> 6 & 0x3F;
				int localY = hash & 0x3F;
				int height = hash >> 12 & 0x3;

				int attributeHashCode = gameObjectBuffer.get() & 0xFF;
				ObjectType type = ObjectType.forId(attributeHashCode >> 2);
				ObjectOrientation orientation = ObjectOrientation.forId(attributeHashCode & 0x3);
				Position position = new Position(x + localX, y + localY, height);

				gameObjectDecoded(id, orientation, type, position);
			}
		}
	}

	/**
	 * Loads all of the map indexes entries and decodes each.
	 *
	 * @param mapBuffer The uncompressed map entry data buffer.
	 * @param x The x coordinate of this map entry.
	 * @param y The y coordinate of this map entry.
	 */
	private void parseTerrain(ByteBuffer mapBuffer, int x, int y) {
		for (int height = 0; height < 4; height++) {
			for (int localX = 0; localX < 64; localX++) {
				for (int localY = 0; localY < 64; localY++) {
					Position position = new Position(x + localX, y + localY, height);

					int flags = 0;
					for (;;) {
						int attributeId = mapBuffer.get() & 0xFF;
						if (attributeId == 0) {
							terrainDecoded(flags, position);
							break;
						} else if (attributeId == 1) {
							mapBuffer.get();
							terrainDecoded(flags, position);
							break;
						} else if (attributeId <= 49) {
							mapBuffer.get();
						} else if (attributeId <= 81) {
							flags = attributeId - 49;
						}
					}
				}
			}
		}
	}

	/**
	 * Decodes the terrains {@link Position}.
	 *
	 * @param flags The flags for the specified position.
	 * @param position The decoded position.
	 */
	private void terrainDecoded(int flags, Position position) {
		if ((flags & FLAG_BLOCKED) != 0) {
			world.getTraversalMap().markBlocked(position.getHeight(), position.getX(), position.getY());
		}

		if ((flags & FLAG_BRIDGE) != 0) {
			world.getTraversalMap().markBridge(position.getHeight(), position.getX(), position.getY());
		}
	}

	/**
	 * Decodes a {@link GameObject} with the specified attributes on the
	 * specified {@link Position}.
	 *
	 * @param id The id of the game object.
	 * @param orientation The orientation of the game object.
	 * @param type The type of the game object.
	 * @param position The position the game object lies on.
	 */
	private void gameObjectDecoded(int id, ObjectOrientation orientation, ObjectType type, Position position) {
		TraversalMap traversalMap = world.getTraversalMap();
		GameObjectDefinition def = GameObjectDefinition.forId(id);

		if (type == GROUND_PROP) {
			if (def.hasActions()) {
				traversalMap.markBlocked(position.getHeight(), position.getX(), position.getY());
			}
		} else if (type == GENERAL_PROP || type == WALKABLE_PROP) {
			if (def.getSize() > 0 || !def.isSolid()) {
				traversalMap.markBlocked(position.getHeight(), position.getX(), position.getY());
			}
		} else if (type.getId() >= 12) {
			traversalMap.markBlocked(position.getHeight(), position.getX(), position.getY());
		} else if (type.getGroup() == WALL) {
			traversalMap.markWall(orientation, position.getHeight(), position.getX(), position.getY(), type, def.isWalkable());
		} else if (type == DIAGONAL_WALL) {
			traversalMap.markBlocked(position.getHeight(), position.getX(), position.getY());
		}

		gameObjects.add(new GameObject(id, position, world, type, orientation));
	}

}