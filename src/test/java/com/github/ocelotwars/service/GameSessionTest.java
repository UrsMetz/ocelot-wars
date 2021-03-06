package com.github.ocelotwars.service;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.github.ocelotwars.engine.Command;
import com.github.ocelotwars.engine.Player;
import com.github.ocelotwars.engine.command.GatherCommand;
import com.github.ocelotwars.engine.command.MoveCommand;
import com.github.ocelotwars.engine.game.Game;
import com.github.ocelotwars.service.commands.Direction;
import com.github.ocelotwars.service.commands.Gather;
import com.github.ocelotwars.service.commands.Move;
import com.github.ocelotwars.service.commands.Unload;

import io.vertx.core.http.ServerWebSocket;
import rx.Observable;
import rx.plugins.RxJavaHooks;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class GameSessionTest {

	@Rule
	public MockitoRule mockitoJUnit = MockitoJUnit.rule();

	private TestScheduler scheduler;

	@Mock
	private Game game;

	private List<SocketPlayer> players = new ArrayList<>();

	@Before
	public void before() {
		scheduler = new TestScheduler();
		RxJavaHooks.setOnComputationScheduler(d -> scheduler);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_CommandsMove() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		Commands commands = new Commands(asList(new Move(1, Direction.EAST)));
		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket1, commands));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game).execute(matchcommands.capture());
		assertThat(matchcommands.getValue(), contains(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.EAST)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_CommandsGather() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		Commands commands = new Commands(asList(new Gather(1)));
		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket1, commands));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game).execute(matchcommands.capture());
		assertThat(matchcommands.getValue(), contains(new GatherCommand(new Player("player1"), 1)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_TwoPlayers_CommandsForPlayer1() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands = new Commands(asList(new Gather(1)));
		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket1, commands));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game).execute(matchcommands.capture());
		assertThat(matchcommands.getValue(), contains(new GatherCommand(new Player("player1"), 1)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_TwoPlayers_CommandsForPlayer2() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands = new Commands(asList(new Gather(1)));
		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket2, commands));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game).execute(matchcommands.capture());
		assertThat(matchcommands.getValue(), contains(new GatherCommand(new Player("player2"), 1)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_TwoPlayers_CommandsForPlayer12() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands1 = new Commands(asList(new Move(1, Direction.NORTH)));
		Commands commands2 = new Commands(asList(new Gather(1)));
		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket2, commands2), new SocketMessage(socket1, commands1));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game, times(2)).execute(matchcommands.capture());
		List<List<Command>> allValues = matchcommands.getAllValues();
		assertThat(allValues, containsInAnyOrder(
			asList(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.NORTH)),
			asList(new GatherCommand(new Player("player2"), 1))));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_TwoPlayers_CommandsForIllegalSecondMessage_() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands1 = new Commands(asList(new Move(1, Direction.NORTH)));
		Commands commands2 = new Commands(asList(new Gather(1)));
		Commands commands3 = new Commands(asList(new Unload(1)));
		Observable<SocketMessage> mq = Observable.just(
			new SocketMessage(socket2, commands2),
			new SocketMessage(socket1, commands1),
			new SocketMessage(socket2, commands3));
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);

		verify(game, times(2)).execute(matchcommands.capture());
		List<List<Command>> allValues = matchcommands.getAllValues();
		assertThat(allValues, containsInAnyOrder(
			asList(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.NORTH)),
			asList(new GatherCommand(new Player("player2"), 1))));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRound_TwoPlayers_CommandsWithTimeout() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands1 = new Commands(asList(new Move(1, Direction.NORTH)));
		Commands commands2 = new Commands(asList(new Gather(1)));

		Observable<SocketMessage> mq = Observable.just(new SocketMessage(socket1, commands1), new SocketMessage(socket2, commands2))
			.zipWith(Observable.interval(3, TimeUnit.SECONDS), (message, time) -> message);
		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.round(0, mq);
		scheduler.advanceTimeBy(6, TimeUnit.SECONDS);

		verify(game, times(1)).execute(matchcommands.capture());
		List<List<Command>> allValues = matchcommands.getAllValues();
		assertThat(allValues, containsInAnyOrder(
			asList(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.NORTH))));
	}

	@Test
	public void testRound_notifysAllPlayers() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Observable<SocketMessage> mq = Observable.empty();
		GameSession session = new GameSession(game, players, 5);

		session.round(0, mq);

		verify(socket1).writeFinalTextFrame("{\"@type\":\"Notify\"}");
		verify(socket2).writeFinalTextFrame("{\"@type\":\"Notify\"}");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRounds_2rounds_runInSequence() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands1 = new Commands(asList(new Move(1, Direction.NORTH)));
		Commands commands2 = new Commands(asList(new Move(1, Direction.WEST)));

		PublishSubject<SocketMessage> mq = PublishSubject.create();

		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);

		session.rounds(2, mq);

		scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
		verify(socket1).writeFinalTextFrame("{\"@type\":\"Notify\"}");
		verify(socket2).writeFinalTextFrame("{\"@type\":\"Notify\"}");
		scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
		mq.onNext(new SocketMessage(socket1, commands1));

		verify(game, times(1)).execute(matchcommands.capture());
		List<List<Command>> commandsRound1 = matchcommands.getAllValues();
		assertThat(commandsRound1, containsInAnyOrder(
			asList(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.NORTH))));

		Mockito.reset(game, socket1, socket2);
		matchcommands = ArgumentCaptor.forClass(List.class);

		scheduler.advanceTimeBy(3, TimeUnit.SECONDS);
		verify(socket1).writeFinalTextFrame("{\"@type\":\"Notify\"}");
		verify(socket2).writeFinalTextFrame("{\"@type\":\"Notify\"}");
		mq.onNext(new SocketMessage(socket2, commands2));

		verify(game, times(1)).execute(matchcommands.capture());
		List<List<Command>> commandsRound2 = matchcommands.getAllValues();
		assertThat(commandsRound2, containsInAnyOrder(
			asList(new MoveCommand(new Player("player2"), 1, com.github.ocelotwars.engine.Direction.WEST))));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRounds_2rounds_terminatesWithWinner() throws Exception {
		ServerWebSocket socket1 = Mockito.mock(ServerWebSocket.class);
		ServerWebSocket socket2 = Mockito.mock(ServerWebSocket.class);
		players.add(new SocketPlayer("player1", socket1));
		players.add(new SocketPlayer("player2", socket2));
		Commands commands1 = new Commands(asList(new Move(1, Direction.NORTH)));
		Commands commands2 = new Commands(asList(new Move(1, Direction.WEST)));

		PublishSubject<SocketMessage> mq = PublishSubject.create();

		GameSession session = new GameSession(game, players, 5);
		ArgumentCaptor<List<Command>> matchcommands = ArgumentCaptor.forClass(List.class);
		SocketPlayer[] winner = new SocketPlayer[1];
		session.winner().subscribe(p -> {winner[0] = p;});
		
		session.rounds(2, mq);

		scheduler.advanceTimeBy(3, TimeUnit.SECONDS);
		mq.onNext(new SocketMessage(socket1, commands1));
		scheduler.advanceTimeBy(3, TimeUnit.SECONDS);
		mq.onNext(new SocketMessage(socket2, commands2));

		verify(game, times(2)).execute(matchcommands.capture());
		List<List<Command>> commandsRound2 = matchcommands.getAllValues();
		assertThat(commandsRound2, contains(
			asList(new MoveCommand(new Player("player1"), 1, com.github.ocelotwars.engine.Direction.NORTH)),
			asList(new MoveCommand(new Player("player2"), 1, com.github.ocelotwars.engine.Direction.WEST))));
		
		assertThat(winner[0], nullValue());
		
		scheduler.advanceTimeBy(4, TimeUnit.SECONDS);
		
		assertThat(winner[0], notNullValue());
	}

}
