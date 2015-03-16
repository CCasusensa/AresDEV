package gm.server.handler;

import gm.GMPacketCreator;
import gm.GMPacketHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.server.Server;
import net.server.channel.Channel;

import org.apache.mina.core.session.IoSession;

import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;

/**
 *
 * @author kevintjuh93
 */
public class PlayerListHandler implements GMPacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea,
			IoSession session) {
		final List<String> playerList = new ArrayList<String>();
		for (final Channel ch : Server.getInstance().getAllChannels()) {
			final Collection<MapleCharacter> list = ch.getPlayerStorage()
					.getAllCharacters();
			synchronized (list) {
				for (final MapleCharacter chr : list) {
					if (!chr.isGM()) {
						playerList.add(chr.getName());
					}
				}
			}
		}
		session.write(GMPacketCreator.sendPlayerList(playerList));
	}
}
