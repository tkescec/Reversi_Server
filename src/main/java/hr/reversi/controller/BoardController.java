package hr.reversi.controller;

import hr.reversi.Main;
import hr.reversi.model.*;
import hr.reversi.rmi.server.ChatService;
import hr.reversi.service.AlertService;
import hr.reversi.service.DocumentationService;
import hr.reversi.ui.DiscUI;
import hr.reversi.util.AlertType;
import hr.reversi.util.DiscState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class BoardController implements Initializable {
    @FXML
    private Circle blackDiscPoints;
    @FXML
    private Circle whiteDiscPoints;
    @FXML
    private Label lbPlayerWhiteName;
    @FXML
    private Label lbPlayerBlackName;
    @FXML
    private Label lbPlayerWhitePoints;
    @FXML
    private Label lbPlayerBlackPoints;
    @FXML
    private GridPane boardGrid;
    @FXML
    private TextArea messages;
    @FXML
    private TextField message;

    private DiscUI discUi;
    private static Socket clientSocket;
    private static BoardState boardState;
    private static Board board;
    private static Player playerWhite;
    private static Player playerBlack;
    private static  DiscState playerTurn;
    private final Boolean discMarker = true;
    private final Boolean moveMarker = true;
    private final String fileName = "reversi.ser";
    private ChatService stub = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startGame();
    }

    /** Start game. */
    private void startGame() {

        board = new Board();
        discUi = new DiscUI();
        boardState = new BoardState();

        startChat();
        startServer();
    }

    private void startChat(){
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            stub = (ChatService) registry.lookup(ChatService.REMOTE_OBJECT_NAME);

            new Thread(() ->  checkForNewMessages()).start();

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void checkForNewMessages(){
        while (true){
            try {
                List<String> chatHistory = stub.getChatHistory();

                StringBuilder chatHistoryBuilder = new StringBuilder();

                for(String message : chatHistory) {
                    chatHistoryBuilder.append(message);
                    chatHistoryBuilder.append("\n");
                }

                messages.setText(chatHistoryBuilder.toString());
                System.out.println("Check for messages at: " + LocalDateTime.now());
                Thread.sleep(2000);
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(Server.PORT)){
            System.err.println("Server listening on port: " + serverSocket.getLocalPort());

            initWhitePlayer();

            clientSocket = serverSocket.accept();
            System.err.println("Client connected from port: " + clientSocket.getPort());
            // outer try catch blocks cannot handle the anonymous implementations
            //new Thread(() ->  processPrimitiveClient(clientSocket)).start();
            new Thread(() ->  processClient(clientSocket)).start();

        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processClient(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());){

            boardState = (BoardState)ois.readObject();
            System.out.println("Retrive data from client");

            if (boardState.getPlayerTurn() == DiscState.white){
                if (playerBlack == null){
                    initBlackPlayer(boardState.getPlayerBlack());
                }
                playerTurn = DiscState.white;
                board.setBoardGrid(boardState.getBoardGrid());

                Platform.runLater(()->{
                    initBoard();
                    initEventHandlers();
                });
            }

            boardState.setPlayerWhite(playerWhite);
            boardState.setPlayerTurn(playerTurn);

            oos.writeObject(getBoardState());
            System.out.println("Send data to client");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Init white player. */
    private void initWhitePlayer() {
        playerWhite = StartGameController.getPlayerOne();
        playerWhite.setAllPlayedMoves(new ArrayList<Integer[]>());
        lbPlayerWhiteName.setText(playerWhite.getName());
    }

    /** Init black player. */
    private void initBlackPlayer(Player player) {
        playerBlack = player;
        playerBlack.setAllPlayedMoves(player.getAllPlayedMoves());
        lbPlayerBlackName.setText(playerBlack.getName());
    }

    /** Init game board. */
    private void initBoard() {
        board.initBoard();
        board.getValidMoves(playerTurn);
        updateBoardView();
    }

    /** Init all events. */
    private void initEventHandlers() {
        onGridClick();
    }

    // ************** HELPER FUNCTIONS **************

    /** Sets player turn.
     *
     * @param value
     */
    private void setPlayerTurn(final DiscState value) {
        this.playerTurn = value;
    }

    /** Changes player turn. */
    private void changePlayerTurn() {
        if (playerTurn.equals(DiscState.white)) {
            setPlayerTurn(DiscState.black);
        } else if (playerTurn.equals(DiscState.black)) {
            setPlayerTurn(DiscState.white);
        }
    }

    /** Counts points for given player.
     *
     * @param player Player class object.
     */
    private void countPlayerPoints(final Player player) {
        DiscState discState = player.getDiscState();
        player.setPoints(board.getAllPlayerDiscs(discState).size());
    }

    /** Adds highlight effect to points counter disc.
     *
     * @param playerTurn current player.
     */
    public void highLightPoints(final DiscState playerTurn) {
        final double opacity = 1.0;
        if (playerTurn == DiscState.white) {
            blackDiscPoints.setStrokeWidth(0);
            whiteDiscPoints.setStrokeWidth(5);
            whiteDiscPoints.setStroke(Color.web("#ed7753", opacity));
        } else if (playerTurn == DiscState.black) {
            whiteDiscPoints.setStrokeWidth(0);
            blackDiscPoints.setStrokeWidth(5);
            blackDiscPoints.setStroke(Color.web("#ed7753", opacity));
        }
    }

    // ************** VIEW UPDATE **************

    /** Updates view of all elements in main window.
     *
     */
    private void updateBoardView() {

        switchOnNoValidMoves();

        for (Node square : boardGrid.getChildren()) {

            Integer col = boardGrid.getColumnIndex(square);
            Integer row = boardGrid.getRowIndex(square);
            DiscState discState = board.getDiscFromBoard(row, col).getState();
            StackPane sp = (StackPane) square;

            if (sp.getChildren().size() == 2) {
                sp.getChildren().remove(1);
            }


            if (discMarker.equals(true)) {
                DiscUI.DiscMarker dm = discUi.new DiscMarker();
                sp.getChildren().add(discUi.makeDisc(discState));

                for (Disc disc : board.getFlipedDiscsToMark()) {
                    int discRow = disc.getRow();
                    int discCol = disc.getCol();
                    if (discRow == row && discCol == col) {
                        StackPane spWithMarker = new StackPane();
                        sp.getChildren().remove(1);
                        spWithMarker.getChildren().addAll(
                                discUi.makeDisc(discState),
                                dm.flipDiscMarker());
                        sp.getChildren().add(spWithMarker);
                    }
                }
            } else if (discMarker.equals(false)) {
                sp.getChildren().add(discUi.makeDisc(discState));
            }

            if (moveMarker.equals(true)) {
                DiscUI.MoveMarker mm = discUi.new MoveMarker();

                for (Integer[] move : board.getAllValidMoves()) {
                    int validMoveRow = move[0];
                    int validMoveCol = move[1];
                    if (row == validMoveRow && col == validMoveCol) {
                        sp.getChildren().add(mm.validMoveMarker());
                    }
                }
            }

            if (sp.getChildren().size() > 2) {
                sp.getChildren().remove(1, sp.getChildren().size() - 1);
            }
        }

        updatePointsCounters();

        if (board.getAllValidMoves().isEmpty()) {
            finishGame(playerWhite, playerBlack);
        }

        board.clearFlipedDiscsToMark();
    }

    /** Switches player if there is no valid move. */
    private void switchOnNoValidMoves() {
        // switch player if there are no valid moves
        if (board.getAllValidMoves().isEmpty()) {
            changePlayerTurn();
            updatePointsCounters();
            board.getValidMoves(playerTurn);
        }
    }

    /** Updates counter for current player. */
    private void updatePointsCounters() {
        countPlayerPoints(playerWhite);
        countPlayerPoints(playerBlack);

        lbPlayerWhitePoints.setText(Integer.toString(playerWhite.getPoints()));
        lbPlayerBlackPoints.setText(Integer.toString(playerBlack.getPoints()));

        highLightPoints(playerTurn);
    }

    /** Runs game updates after placed move.
     *
     * @param row row coordinates.
     * @param col column coordinates.
     */
    private void runOnClick(final Integer row, final Integer col) throws Exception {

        boolean validMove = validatePlacedMove(row, col);

        // player can place disc only on empty square
        if (Boolean.TRUE.equals(validMove)) {
            board.modifyDiscState(row, col, playerTurn);
            board.flipAllDiscs(row, col, playerTurn);

            // record player move
            recordPlayerMove(row, col, playerTurn);

            // change player after update
            changePlayerTurn();

            board.getValidMoves(playerTurn);

            updateBoardView();

            if (clientSocket.isConnected()){
                sendDataToClient(clientSocket);
            }
        }
    }

    private void sendDataToClient(Socket clientSocket) {
        try{
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());

//            if (boardState.getPlayerTurn() == DiscState.white){
//                if (playerBlack == null){
//                    initBlackPlayer(boardState.getPlayerBlack());
//                }
//                playerTurn = DiscState.white;
//                board.setBoardGrid(boardState.getBoardGrid());
//
//                Platform.runLater(()->{
//                    initBoard();
//                    initEventHandlers();
//                });
//            }
//
//            boardState.setPlayerWhite(playerWhite);
//            boardState.setPlayerTurn(playerTurn);

            oos.writeObject(getBoardState());
            System.out.println("Send data to client");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**  Checks if placed move is on list.
     *
     * @param row row coordinate.
     * @param col column coordinate.
     * @param playerTurn current player.
     * @return boolean value.
     */
    private void recordPlayerMove(Integer row, Integer col, DiscState playerTurn) throws Exception {

        Integer[] move = new Integer[2];
        move[0] = row;
        move[1] = col;

        switch (playerTurn) {
            case white -> {
                playerWhite.addPlayedMove(move);
            }
            case black -> {
                playerBlack.addPlayedMove(move);
            }
            default -> {
                throw new Exception("Wrong player!");
            }
        }
    }

    /**  Finish game and switch to result view.
     *
     * @param playerWhite player with white disc.
     * @param playerBlack player with black disc.
     * @return boolean value.
     */
    private void finishGame(Player playerWhite, Player playerBlack) {
        checkWhichPlayerWon(playerWhite, playerBlack);
        showResultsView();
    }

    /** Set board state */
    private BoardState getBoardState() {

        boardState.setBoardGrid(board.getBoardGrid());
        boardState.setPlayerTurn(playerTurn);
        boardState.setPlayerWhite(playerWhite);
        boardState.setPlayerBlack(playerBlack);

        return boardState;
    }
    /** Save game. */
    private void saveGame() throws IOException, FileNotFoundException {
        BoardState boardState = getBoardState();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(boardState);
        }

        String alertContent = "Game saved successfully!";
        AlertService.showAlert(AlertType.info, alertContent);
    }
    /** Load game. */
    private void loadGame() throws IOException, ClassNotFoundException {

        String alertContent = "Are you sure?";
        Optional<ButtonType> result = AlertService.showAlert(AlertType.confirm, alertContent);

        if (result.get() == ButtonType.OK){
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                BoardState boardState = (BoardState) ois.readObject();

                loadPlayers(boardState);


                board.setBoardGrid(boardState.getBoardGrid());
                board.getValidMoves(playerTurn);

                updateBoardView();
            } catch (FileNotFoundException e){
                AlertService.showAlert(AlertType.error, e.getMessage());
            }
        } else {
            // ... user chose CANCEL or closed the dialog
        }

    }
    /** Load players state. */
    private void loadPlayers(BoardState boardState) {
        playerWhite = boardState.getPlayerWhite();
        playerBlack = boardState.getPlayerBlack();
        playerTurn = boardState.getPlayerTurn();

        playerWhite.setAllPlayedMoves(boardState.getPlayerWhite().getAllPlayedMoves());
        playerBlack.setAllPlayedMoves(boardState.getPlayerBlack().getAllPlayedMoves());

        lbPlayerWhiteName.setText(playerWhite.getName());
        lbPlayerBlackName.setText(playerBlack.getName());
    }

    /**  Show who won in alert box.
     *
     * @param playerWhite player with white disc.
     * @param playerBlack player with black disc.
     * @return boolean value.
     */
    private void checkWhichPlayerWon(Player playerWhite, Player playerBlack) {

        if (playerWhite.getPoints() > playerBlack.getPoints()){
            playerWhite.setWinner(true);
            showAlert(playerWhite);
        } else {
            playerBlack.setWinner(true);
            showAlert(playerBlack);
        }
    }

    /** Switch to results view. */
    private void showResultsView() {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/hr/reversi/view/result-view.fxml"));
        Scene scene = null;

        try {
            scene = new Scene(fxmlLoader.load(), 800, 600);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Stage stage = Main.getMainStage();

        stage.setTitle("Reversi | Results");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /** Switch to high score view. */
    public static void showHighScoreView(){
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/hr/reversi/view/high-score-view.fxml"));
        Scene scene = null;

        try {
            scene = new Scene(fxmlLoader.load(), 600, 600);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Stage stage = Main.getMainStage();

        stage.setTitle("Reversi | High Score");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /**  Checks if placed move is on list.
     *
     * @param row row coordinate.
     * @param col column coordinate.
     * @return boolean value.
     */
    private boolean validatePlacedMove(final Integer row, final Integer col) {
        boolean result = false;
        for (Integer[] move : board.getAllValidMoves()) {
            int validMoveRow = move[0];
            int validMoveCol = move[1];
            if (row == validMoveRow && col == validMoveCol) {
                result = true;
            }
        }
        return result;
    }

    /**  Get white player.
     *
     * @return Player playerWhite.
     */
    public static Player getPlayerWhite() {
        return playerWhite;
    }

    /**  Get black player.
     *
     * @return Player playerBlack.
     */
    public static Player getPlayerBlack() {
        return playerBlack;
    }

    // ************** EVENT HANDLERS **************

    /** Click handler for placed move. */
    private void onGridClick() {
        boardGrid.getChildren().forEach(square -> {
            square.setOnMouseClicked(event -> {
                Node node = (Node) event.getSource();
                Integer col = boardGrid.getColumnIndex(node);
                Integer row = boardGrid.getRowIndex(node);

                try {
                    if (playerTurn == DiscState.white){
                        runOnClick(row, col);
                    }
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(e.getMessage().toString());
                    alert.show();
                }
            });
        });
    }

    @FXML
    /** Send message button click handler. */
    private void onSendMessage() {
        try {
            stub.sendMessage(playerWhite.getName()
                    + " > " + message.getText());

            List<String> chatHistory = stub.getChatHistory();

            StringBuilder chatHistoryBuilder = new StringBuilder();

            for(String message : chatHistory) {
                chatHistoryBuilder.append(message);
                chatHistoryBuilder.append("\n");
            }

            messages.setText(chatHistoryBuilder.toString());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    /** New game button click handler. */
    private void onNewGameButtonClick() {
        startGame();
    }
    @FXML
    /** Save game button click handler. */
    private void onSaveGameButtonClick() throws IOException {
        saveGame();
    }

    @FXML
    /** Load game button click handler. */
    private void onLoadGameButtonClick() throws IOException, ClassNotFoundException {
        loadGame();
    }

    @FXML
    /** High score button click handler. */
    private void onHighScoreButtonClick() {
        showHighScoreView();
    }
    @FXML
    /** Documentation button click handler. */
    private void onDocumentationButtonClick() {
        DocumentationService.generateDocumentation();
    }

    //*********************************************
    private void showAlert(Player player) {
        if (player.isWinner()){
            String alertContent = "Player " + player.getName() + " won!";
            AlertService.showAlert(AlertType.info, alertContent);
        }
    }
}
