package org.apollo.net.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apollo.game.GameService;
import org.apollo.game.model.Player;
import org.apollo.game.model.World.RegistrationStatus;
import org.apollo.io.player.PlayerSerializerResponse;
import org.apollo.net.NetworkConstants;
import org.apollo.net.codec.game.GameMessageDecoder;
import org.apollo.net.codec.game.GameMessageEncoder;
import org.apollo.net.codec.game.GamePacketDecoder;
import org.apollo.net.codec.game.GamePacketEncoder;
import org.apollo.net.codec.login.LoginConstants;
import org.apollo.net.codec.login.LoginRequest;
import org.apollo.net.codec.login.LoginResponse;
import org.apollo.security.IsaacRandomPair;

/**
 * A login session.
 *
 * @author Graham
 */
public final class LoginSession extends Session {

	/**
	 * The regular expression pattern used to determine valid credentials.
	 */
	private static final Pattern PATTERN = Pattern.compile("\\w(\\w| (?! )){2,10}\\w");

	/**
	 * The current version number.
	 */
	public static final int VERSION = 317;

	/**
	 * The game service.
	 */
	private final GameService gameService;

	/**
	 * Creates a login session for the specified channel handler context.
	 *
	 * @param ctx The channels context.
	 * @param gameService The game service.
	 */
	public LoginSession(ChannelHandlerContext ctx, GameService gameService) {
		super(ctx);
		this.gameService = gameService;
	}

	@Override
	public void messageReceived(Object message) throws Exception {
		if (message instanceof LoginRequest) {
			handleLoginRequest((LoginRequest) message);
		}
	}

	/**
	 * Handles a login request.
	 *
	 * @param request The login request.
	 * @throws IOException If some I/O error occurs.
	 */
	private void handleLoginRequest(LoginRequest request) throws IOException {
		int code = LoginConstants.STATUS_OK;

		if (requiresUpdate(request)) {
			code = LoginConstants.STATUS_GAME_UPDATED;
		} else if (badCredentials(request)) {
			code = LoginConstants.STATUS_INVALID_CREDENTIALS;
		}

		if (code == LoginConstants.STATUS_OK) {
			gameService.getSerializerWorker().submitLoadRequest(this, request, gameService.getFileSystem());
		} else {
			handlePlayerLoaderResponse(request, new PlayerSerializerResponse(code));
		}
	}

	/**
	 * Checks if an update is required whenever a {@link Player} submits a login
	 * request.
	 *
	 * @param request The login request.
	 * @return {@code true} if an update is required, otherwise return
	 *         {@code false}.
	 * @throws IOException If some I/O exception occurs.
	 */
	private boolean requiresUpdate(LoginRequest request) throws IOException {
		if (VERSION != request.getCurrentVersion()) {
			return true;
		}

		ByteBuffer buffer = gameService.getFileSystem().getArchiveHashes();

		int[] clientCrcs = request.getArchiveCrcs();
		int[] serverCrcs = new int[clientCrcs.length];

		if (buffer.remaining() < clientCrcs.length) {
			return true;
		}

		Arrays.setAll(serverCrcs, crc -> buffer.getInt());

		return !Arrays.equals(clientCrcs, serverCrcs);
	}

	/**
	 * Returns {@code true} if the credentials within the specified login
	 * request are invalid otherwise {@code false}.
	 *
	 * @param request The login request.
	 * @return {@code true} if and only if the specified login requests'
	 *         credentials were invalid, otherwise {@code false}.
	 */
	private boolean badCredentials(LoginRequest request) {
		String username = request.getCredentials().getUsername();
		String password = request.getCredentials().getPassword();

		Matcher usernameMatcher = PATTERN.matcher(username);
		Matcher passwordMatcher = PATTERN.matcher(password);

		return !(usernameMatcher.matches() && passwordMatcher.matches());
	}

	/**
	 * Handles a response from the login service.
	 *
	 * @param request The request this response corresponds to.
	 * @param response The response.
	 */
	public void handlePlayerLoaderResponse(LoginRequest request, PlayerSerializerResponse response) {
		int status = response.getStatus();
		Player player = response.getPlayer();
		int rights = player == null ? 0 : player.getPrivilegeLevel().toInteger();
		boolean flagged = player != null && player.isFlagged();

		if (player != null) {
			GameSession session = new GameSession(ctx(), player, gameService);
			player.setSession(session, request.isReconnecting());

			RegistrationStatus registrationStatus = gameService.registerPlayer(player);

			if (registrationStatus != RegistrationStatus.OK) {
				if (registrationStatus == RegistrationStatus.ALREADY_ONLINE) {
					status = LoginConstants.STATUS_ACCOUNT_ONLINE;
				} else {
					status = LoginConstants.STATUS_SERVER_FULL;
				}

				player = null;
				rights = 0;
				flagged = false;
			}
		}

		Channel channel = ctx().channel();
		ChannelFuture future = channel.writeAndFlush(new LoginResponse(status, rights, flagged));

		if (player != null) {
			IsaacRandomPair randomPair = request.getRandomPair();

			channel.pipeline().addFirst("messageEncoder", new GameMessageEncoder(gameService.getMessageTranslator()));
			channel.pipeline().addBefore("messageEncoder", "gameEncoder", new GamePacketEncoder(randomPair.getEncodingRandom()));

			channel.pipeline().addBefore("handler", "gameDecoder", new GamePacketDecoder(randomPair.getDecodingRandom()));
			channel.pipeline().addAfter("gameDecoder", "messageDecoder", new GameMessageDecoder(gameService.getMessageTranslator()));

			channel.pipeline().remove("loginDecoder");
			channel.pipeline().remove("loginEncoder");

			ctx().attr(NetworkConstants.NETWORK_SESSION).set(player.getSession());
		} else {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

}