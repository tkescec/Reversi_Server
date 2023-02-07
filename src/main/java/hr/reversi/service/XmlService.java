package hr.reversi.service;

import hr.reversi.model.BoardState;
import javafx.scene.control.Alert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class XmlService {
    private static final String pathName = "moves.xml";

    public static void writeXml(BoardState boardState){
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmlDocument = documentBuilder.newDocument();

            Element playersRoot = xmlDocument.createElement("Players");

            // White player
            Element whitePlayer = xmlDocument.createElement("WhitePlayer");

            Element whitePlayerName = xmlDocument.createElement("Name");

            Node whitePlayerNameNode = xmlDocument.createTextNode(boardState.getPlayerWhite().getName());

            whitePlayerName.appendChild(whitePlayerNameNode);
            whitePlayer.appendChild(whitePlayerName);

            Element whitePlayerMoves = xmlDocument.createElement("Moves");
            boardState.getPlayerWhite().getAllPlayedMoves().forEach(move -> {
                Element whitePlayerMove = xmlDocument.createElement("Move");

                Element whitePlayerMoveRow = xmlDocument.createElement("Row");
                Node whitePlayerMoveRowNode = xmlDocument.createTextNode(move[0].toString());
                whitePlayerMoveRow.appendChild(whitePlayerMoveRowNode);
                whitePlayerMove.appendChild(whitePlayerMoveRow);

                Element whitePlayerMoveCol = xmlDocument.createElement("Col");
                Node whitePlayerMoveColNode = xmlDocument.createTextNode(move[1].toString());
                whitePlayerMoveCol.appendChild(whitePlayerMoveColNode);
                whitePlayerMove.appendChild(whitePlayerMoveCol);

                whitePlayerMoves.appendChild(whitePlayerMove);
            });
            whitePlayer.appendChild(whitePlayerMoves);

            playersRoot.appendChild(whitePlayer);


            // Black player
            Element blackPlayer = xmlDocument.createElement("BlackPlayer");

            Element blackPlayerName = xmlDocument.createElement("Name");

            Node blackPlayerNameNode = xmlDocument.createTextNode(boardState.getPlayerBlack().getName());

            blackPlayerName.appendChild(blackPlayerNameNode);
            blackPlayer.appendChild(blackPlayerName);

            Element blackPlayerMoves = xmlDocument.createElement("Moves");
            boardState.getPlayerBlack().getAllPlayedMoves().forEach(move -> {
                Element blackPlayerMove = xmlDocument.createElement("Move");

                Element blackPlayerMoveRow = xmlDocument.createElement("Row");
                Node blackPlayerMoveRowNode = xmlDocument.createTextNode(move[0].toString());
                blackPlayerMoveRow.appendChild(blackPlayerMoveRowNode);
                blackPlayerMove.appendChild(blackPlayerMoveRow);

                Element blackPlayerMoveCol = xmlDocument.createElement("Col");
                Node blackPlayerMoveColNode = xmlDocument.createTextNode(move[1].toString());
                blackPlayerMoveCol.appendChild(blackPlayerMoveColNode);
                blackPlayerMove.appendChild(blackPlayerMoveCol);

                blackPlayerMoves.appendChild(blackPlayerMove);
            });
            blackPlayer.appendChild(blackPlayerMoves);

            playersRoot.appendChild(blackPlayer);

            xmlDocument.appendChild(playersRoot);


            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            Source xmlSource = new DOMSource(xmlDocument);
            Result xmlResult = new StreamResult(new File(pathName));

            transformer.transform(xmlSource, xmlResult);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("XML file created!");
            alert.setHeaderText("XML file was successfuly created!");
            alert.setContentText("File '" + pathName + "' was created!");

            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void readXml(){
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document xmlDocument = parser.parse(new File(pathName));

            NodeList playersList = xmlDocument.getElementsByTagName("Players");
            Element players = (Element) playersList.item(0);
            Element whitePlayer = (Element) players.getElementsByTagName("WhitePlayer").item(0);
            Element blackPlayer = (Element) players.getElementsByTagName("BlackPlayer").item(0);

            String whitePlayerName = whitePlayer.getElementsByTagName("Name").item(0).getTextContent();
            String blackPlayerName = blackPlayer.getElementsByTagName("Name").item(0).getTextContent();

            Element whitePlayerMoves = (Element)whitePlayer.getElementsByTagName("Moves").item(0);
            Element blackPlayerMoves = (Element)blackPlayer.getElementsByTagName("Moves").item(0);

            System.out.println("White Player Name: " + whitePlayerName);
            System.out.println("White Player Moves:");
            NodeList whitePlayerMove = whitePlayerMoves.getElementsByTagName("Move");
            for(int i = 0; i < whitePlayerMove.getLength(); i++) {
                Node moveNode = whitePlayerMove.item(i);
                if(moveNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element moveElement = (Element) moveNode;

                    String rowText = moveElement
                            .getElementsByTagName("Row")
                            .item(0)
                            .getTextContent();

                    String colText = moveElement
                            .getElementsByTagName("Col")
                            .item(0)
                            .getTextContent();

                    System.out.println("#" + (i + 1) + "-> ROW:" + rowText + " COL:" + colText );
                }
            }
            System.out.println("\n");
            System.out.println("Black Player Name: " + blackPlayerName);
            System.out.println("Black Player Moves:");
            NodeList blackPlayerMove = blackPlayerMoves.getElementsByTagName("Move");
            for(int i = 0; i < blackPlayerMove.getLength(); i++) {
                Node moveNode = blackPlayerMove.item(i);
                if(moveNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element moveElement = (Element) moveNode;

                    String rowText = moveElement
                            .getElementsByTagName("Row")
                            .item(0)
                            .getTextContent();

                    String colText = moveElement
                            .getElementsByTagName("Col")
                            .item(0)
                            .getTextContent();

                    System.out.println("#" + (i + 1) + "-> ROW:" + rowText + " COL:" + colText);
                }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
