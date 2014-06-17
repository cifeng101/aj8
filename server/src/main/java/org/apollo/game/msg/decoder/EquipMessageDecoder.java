package org.apollo.game.msg.decoder;

import org.apollo.game.msg.MessageDecoder;
import org.apollo.game.msg.annotate.DecodesMessage;
import org.apollo.game.msg.impl.EquipMessage;
import org.apollo.net.codec.game.DataTransformation;
import org.apollo.net.codec.game.DataType;
import org.apollo.net.codec.game.GamePacket;
import org.apollo.net.codec.game.GamePacketReader;

/**
 * An {@link MessageDecoder} for the {@link EquipMessage}.
 *
 * @author Graham
 */
@DecodesMessage(41)
public final class EquipMessageDecoder extends MessageDecoder<EquipMessage> {

    @Override
    public EquipMessage decode(GamePacket packet) {
	GamePacketReader reader = new GamePacketReader(packet);
	int id = (int) reader.getUnsigned(DataType.SHORT);
	int slot = (int) reader.getUnsigned(DataType.SHORT, DataTransformation.ADD);
	int interfaceId = (int) reader.getUnsigned(DataType.SHORT, DataTransformation.ADD);
	return new EquipMessage(interfaceId, id, slot);
    }

}
