package com.mindhubweb.salvo.model;

import javax.persistence.*;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Entity
public class GamePlayer {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="gameID")
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="playerID")
    private Player player;

    private LocalDateTime joinDate;

    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Ship> ships = new HashSet<>();

    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Salvo> salvoes = new HashSet<>();

    public GamePlayer() { }

    public GamePlayer(Game game, Player player, LocalDateTime joinDate, Set<Ship> ships, Set<Salvo> salvoes) {
        this.joinDate = joinDate;
        this.game = game;
        this.player = player;
        this.addShips(ships);
        this.addSalvoes(salvoes);
    }

    public Map<String, Object> makeGamePlayerDTO() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getId());
        dto.put("player", this.player.makePlayerDTO());
        dto.put("gameState", gameState());
        dto.put("salvoTurn", currentTurn());
        dto.put("shotsToMake", shotsToMake());
        if (this.getScore() != null)
            dto.put("score", this.getScore().getScorePoint());
        else
            dto.put("score", this.getScore());
        return dto;
    }

    public Map<String, Object> makeGameViewDTO() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.game.getId());
        dto.put("created", this.game.getCreationDate());
        dto.put("gamePlayers", this.game.getGamePlayers().stream().map(GamePlayer::makeGamePlayerDTO));
        dto.put("ships", this.getShips().stream().map(Ship::makeShipDTO));
        dto.put("salvoes", this.game.getGamePlayers().stream().flatMap(gamePlayer -> gamePlayer.getSalvoes().stream().map(Salvo::makeSalvoDTO)));
        return dto;
    }

    public int currentTurn() {
        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGamePlayers().stream().filter(gamePlayer1 -> gamePlayer1.getId() != this.getId()).findFirst();

        if (opponentGamePlayer.isPresent() && this.getSalvoes().size() > opponentGamePlayer.get().getSalvoes().size()) {
            return this.getSalvoes().stream().mapToInt(Salvo::getTurn).max().orElse(0);
        }
        return this.getSalvoes().stream().mapToInt(Salvo::getTurn).max().orElse(0)+1;
    }

    public int shotsToMake() {
        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGamePlayers().stream().filter(gamePlayer1 -> gamePlayer1.getId() != this.getId()).findFirst();
        int totalShips = 5;
        return opponentGamePlayer.map(gamePlayer -> totalShips - this.getSinks(currentTurn()-1, gamePlayer.getSalvoes(), this.getShips()).size()).orElse(totalShips);
    }

    public Enum<GamePlayerState> gameState() {
        Optional <GamePlayer> opponentGamePlayer = this.getGame().getGamePlayers().stream().filter(gamePlayer1 -> gamePlayer1.getId() != this.getId()).findFirst();
        int lastTurn = this.getSalvoes().stream().mapToInt(Salvo::getTurn).max().orElse(0);
        int totalShips = 5;

        Enum<GamePlayerState> response = GamePlayerState.ERROR;

        if (!opponentGamePlayer.isPresent()) {
            response = GamePlayerState.WAIT_OPPONENT;
        } else {
            if (this.getShips().isEmpty()) {
                response = GamePlayerState.PLACE_SHIPS;
            } else if (opponentGamePlayer.get().getShips().isEmpty()) {
                response = GamePlayerState.WAIT_OPPONENT_SHIPS;
            } else {
                if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() < totalShips)) {
                    if ((this.getPlayer().getSide() == Side.DARK) && lastTurn == 4) {
                        response = GamePlayerState.EPIC_WIN;
                    } else {
                        response = GamePlayerState.WIN;
                    }
                } else if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() == totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() == totalShips)) {
                    response = GamePlayerState.DRAW;
                } else if ((this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size()) && (this.getSinks(lastTurn, this.getSalvoes(), opponentGamePlayer.get().getShips()).size() < totalShips) && (this.getSinks(lastTurn, opponentGamePlayer.get().getSalvoes(), this.getShips()).size() == totalShips)) {
                    response = GamePlayerState.LOSE;
                } else if ((this.getId() < opponentGamePlayer.get().getId()) && (this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.ENTER_SALVO;
                } else if ((this.getId() < opponentGamePlayer.get().getId()) && (this.getSalvoes().size() > opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.WAIT_OPPONENT_SALVO;
                } else if ((this.getId() > opponentGamePlayer.get().getId()) && (this.getSalvoes().size() < opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.ENTER_SALVO;
                } else if ((this.getId() > opponentGamePlayer.get().getId()) && (this.getSalvoes().size() == opponentGamePlayer.get().getSalvoes().size())) {
                    response = GamePlayerState.WAIT_OPPONENT_SALVO;
                }
            }
        }
        return response;
    }

    public List<Map<String, Object>> getSinks(int turn, Set <Salvo> salvos, Set<Ship> ships) {
        List<String> allShots = new ArrayList<>();
        salvos
                .stream()
                .filter(salvo -> salvo.getTurn() <= turn)
                .forEach(salvo -> allShots.addAll(salvo.getShots()));
        return ships
                .stream()
                .filter(ship -> allShots.containsAll(ship.getCells()))
                .map(Ship::makeShipDTO)
                .collect(Collectors.toList());
    }


    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public Game getGame() { return game; }

    public void setGame(Game game) { this.game = game; }

    public Player getPlayer() { return player; }

    public void setPlayer(Player player) { this.player = player; }

    public LocalDateTime getJoinDate() { return joinDate; }

    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }

    public Set<Ship> getShips() { return ships; }

    public void setShips(Set<Ship> ships) { this.ships = ships; }

    public void addShip(Ship ship) {
        ship.setGamePlayer(this);
        ships.add(ship);
    }
    public void addShips(Set<Ship> ships) {
        ships.forEach(this::addShip);
    }

    public Set<Salvo> getSalvoes() { return salvoes; }

    public void setSalvoes(Set<Salvo> salvoes) { this.salvoes = salvoes; }

    public void addSalvo(Salvo salvo) {
        salvo.setGamePlayer(this);
        salvoes.add(salvo);
    }
    public void addSalvoes(Set<Salvo> salvoes) {
        salvoes.forEach(this::addSalvo);
    }

    public Score getScore () {
        return this.player.getScore(this.game);
    }
}
