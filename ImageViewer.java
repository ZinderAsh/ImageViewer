import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.*;

public class ImageViewer extends Application {

    private static ArrayList<Album> albums; // Albums of images
    private static ArrayList<Image> images; // Images outside of albums
    private static ArrayList<Viewable> viewables; // List of all images and albums
    private static ArrayList<Viewable> results; // List of all images and albums
    private static int index = 0;
    private static int albumIndex = 0;
    private static int page = 0;
    private static int newPage = 0;
    private boolean fullLoad = false;
    private Album showAlbum = null;
    private boolean slideshowActive = false;
    public static SlideshowTimerTask currentTask = null;
    public static int testInt = 0;

    public static GifDecoder gifDecoder = new GifDecoder();

    private static Document metadata = null;

    private static File dir;

    public static void main(String[] args) {

        albums = new ArrayList<>();
        images = new ArrayList<>();

        if (args.length >= 1) {
            dir = new File(args[0]);
        } else {
            dir = new File("../");
        }

        System.out.println("Loading images...");

        loadDir(dir);

        int total = images.size();
        for (Album a : albums) {
            total += a.size();
        }

        System.out.printf("Loaded %d albums.\nLoaded %d single images.\nLoaded a total of %d images.\n", albums.size(),
                images.size(), total);

        viewables = new ArrayList<>();
        viewables.addAll(albums);
        viewables.addAll(images);
        viewables.sort(new Comparator<Viewable>() {
            @Override
            public int compare(Viewable a, Viewable b) {
                return a.getName().compareTo(b.getName());
            }
        });

        results = new ArrayList<>();
        results.addAll(viewables);

        System.out.println("Loading metadata...");

        if (loadXML()) {
            loadMetadata();
        } else {
            System.out.println("Failed to load metadata.");
        }

        launch();

    }

    public static void loadMetadata() {

        int total = 0;

        org.w3c.dom.Node root = metadata.getElementsByTagName("albums").item(0);
        NodeList nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getNodeName().equals("album")) {
                    String name = element.getElementsByTagName("name").item(0).getTextContent();
                    for (Album a : albums) {
                        if (a.getName().equals(name)) {
                            MetadataAlbum meta = a.getMetadata();
                            meta.setFromXML();
                            // Album Type
                            if (element.getElementsByTagName("type").getLength() > 0) {
                                String type = element.getElementsByTagName("type").item(0).getTextContent();
                                meta.setType(Short.parseShort(type));
                            }
                            // Thumbnail ID
                            if (element.getElementsByTagName("thumb").getLength() > 0) {
                                String thumbnail = element.getElementsByTagName("thumb").item(0).getTextContent();
                                meta.setThumbnailID(Integer.parseInt(thumbnail));
                            }
                            // Slideshow Hidden Images
                            if (element.getElementsByTagName("hide").getLength() > 0) {
                                Element hide = (Element) element.getElementsByTagName("hide").item(0);
                                NodeList ids = hide.getElementsByTagName("id");
                                for (int j = 0; j < ids.getLength(); j++) {
                                    Node n = ids.item(j);
                                    int id = Integer.parseInt(n.getTextContent());
                                    meta.addHideImage(id);
                                }
                            }
                            // Tags
                            if (element.getElementsByTagName("tags").getLength() > 0) {
                                Element tagList = (Element) element.getElementsByTagName("tags").item(0);
                                NodeList tags = tagList.getElementsByTagName("tag");
                                for (int j = 0; j < tags.getLength(); j++) {
                                    Node n = tags.item(j);
                                    String tag = n.getTextContent();
                                    meta.addTag(tag);
                                }
                            }
                            total++;
                            break;
                        }
                    }
                }
            }
        }

        System.out.printf("Loaded Metadata for %d albums.\n", total);

        total = 0;

        root = metadata.getElementsByTagName("images").item(0);
        nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getNodeName().equals("image")) {
                    String name = element.getElementsByTagName("name").item(0).getTextContent();
                    for (Image m : images) {
                        if (m.getName().equals(name)) {
                            MetadataImage meta = m.getMetadata();
                            meta.setFromXML();
                            // Tags
                            if (element.getElementsByTagName("tags").getLength() > 0) {
                                Element tagList = (Element) element.getElementsByTagName("tags").item(0);
                                NodeList tags = tagList.getElementsByTagName("tag");
                                for (int j = 0; j < tags.getLength(); j++) {
                                    Node n = tags.item(j);
                                    String tag = n.getTextContent();
                                    meta.addTag(tag);
                                }
                            }
                            total++;
                            break;
                        }
                    }
                }
            }
        }

        System.out.printf("Loaded Metadata for %d images.\n", total);

    }

    public static boolean loadXML() {
        File xmlFile = new File("metadata.xml");

        DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder xmlBuilder = null;
        try {
            xmlBuilder = xmlFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            System.out.println("ERROR: Failed to create DocumentBuilder. Loading without metadata");
            return false;
        }

        if (xmlBuilder != null) {
            try {
                if (xmlFile.exists()) {
                    metadata = xmlBuilder.parse(xmlFile);
                } else {
                    metadata = xmlBuilder.newDocument();
                    Element rootElement = metadata.createElement("metadata");
                    metadata.appendChild(rootElement);
                    Element imagesElement = metadata.createElement("images");
                    rootElement.appendChild(imagesElement);
                    Element albumsElement = metadata.createElement("albums");
                    rootElement.appendChild(albumsElement);

                    saveMetadata();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public static void saveMetadata() {
        if (metadata != null) {
            File xmlFile = new File("metadata.xml");

            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(metadata);
                StreamResult result = new StreamResult(xmlFile);
                transformer.transform(source, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No metadata to save.");
        }
    }

    public static void loadDir(File dir) {

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (Album.isAlbumDir(f)) {
                    Album album = new Album(f);
                    if (album.getImageCount() == 1) {
                        images.add(album.getImage(0));
                    } else if (album.getImageCount() > 1) {
                        albums.add(album);
                    }
                } else {
                    loadDir(f);
                }
            } else {
                if (Image.isImageFile(f)) {
                    images.add(new Image(f));
                }
            }
        }

    }

    @Override
    public void start(Stage stage) {

        ContextMenu contextMenu = new ContextMenu();

        Rectangle largeImageOverlay = new Rectangle();
        largeImageOverlay.widthProperty().bind(stage.widthProperty());
        largeImageOverlay.heightProperty().bind(stage.heightProperty());
        largeImageOverlay.setFill(new Color(0, 0, 0, 0.9));

        ImageView largeImageView = new ImageView();
        largeImageView.fitWidthProperty().bind(stage.widthProperty().multiply(0.9));
        largeImageView.fitHeightProperty().bind(stage.heightProperty().multiply(0.8));
        largeImageView.setPreserveRatio(true);

        MediaView mediaView = new MediaView(null);
        mediaView.fitWidthProperty().bind(stage.widthProperty().multiply(0.9));
        mediaView.fitHeightProperty().bind(stage.heightProperty().multiply(0.8));
        mediaView.setPreserveRatio(true);

        Label indexLabel = new Label();
        indexLabel.setTextFill(Color.WHITE);
        indexLabel.setFont(new Font("Arial", 12));
        Label nameLabel = new Label();
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(new Font("Arial", 20));
        Label albumIndexLabel = new Label();
        albumIndexLabel.setTextFill(Color.WHITE);
        albumIndexLabel.setFont(new Font("Arial", 20));

        VBox topBox = new VBox(indexLabel, nameLabel);
        topBox.setAlignment(Pos.CENTER);
        HBox bottomBox = new HBox(albumIndexLabel);
        bottomBox.setAlignment(Pos.CENTER);

        BorderPane largeImagePane = new BorderPane();
        largeImagePane.setPadding(new Insets(12, 12, 12, 12));
        largeImagePane.getChildren().add(largeImageOverlay);
        largeImagePane.setCenter(largeImageView);
        largeImagePane.setTop(topBox);
        largeImagePane.setBottom(bottomBox);
        largeImagePane.setVisible(false);

        EventHandler<KeyEvent> handler = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                if (largeImageView.isVisible() && showAlbum == null) {
                    if (mediaView.getMediaPlayer() != null) {
                        MediaPlayer mp = mediaView.getMediaPlayer();
                        mediaView.setMediaPlayer(null);
                        mp.dispose();
                    }
                    boolean isVideo = false;
                    largeImagePane.setCenter(largeImageView);
                    if (e.getCode() == KeyCode.RIGHT) { // Next Image
                        if (index < results.size() - 1) {
                            index++;
                            if (!results.get(index).isAlbum() && ((Image) results.get(index)).isVideo()) {
                                mediaView.setMediaPlayer(((Image) results.get(index)).getMediaPlayerRepeat());
                                largeImagePane.setCenter(mediaView);
                                isVideo = true;
                            } else {
                                largeImageView.setImage(results.get(index).getImage());
                            }
                            albumIndex = 0;
                        }
                    } else if (e.getCode() == KeyCode.LEFT) { // Prev Image
                        if (index > 0) {
                            index--;
                            if (!results.get(index).isAlbum() && ((Image) results.get(index)).isVideo()) {
                                mediaView.setMediaPlayer(((Image) results.get(index)).getMediaPlayerRepeat());
                                largeImagePane.setCenter(mediaView);
                                isVideo = true;
                            } else {
                                largeImageView.setImage(results.get(index).getImage());
                            }
                            albumIndex = 0;
                        }
                    } else if (e.getCode() == KeyCode.DOWN) { // Next Album Image
                        if (results.get(index).isAlbum()) {
                            Album album = (Album) results.get(index);
                            if (albumIndex < album.getImageCount() - 1) {
                                albumIndex++;
                                largeImageView.setImage(album.getImage(albumIndex).getImage());
                            }
                        }
                    } else if (e.getCode() == KeyCode.UP) { // Prev Album Image
                        if (results.get(index).isAlbum()) {
                            Album album = (Album) results.get(index);
                            if (albumIndex > 0) {
                                albumIndex--;
                                largeImageView.setImage(album.getImage(albumIndex).getImage());
                            }
                        }
                    }
                    indexLabel.setText("Image #" + Integer.toString(index));
                    nameLabel.setText(results.get(index).getName());
                    if (results.get(index).isAlbum()) {
                        Album album = (Album) results.get(index);
                        albumIndexLabel.setText(String.format("Page %d/%d", albumIndex + 1, album.getImageCount()));
                    } else {
                        albumIndexLabel.setText("");
                    }
                    if (isVideo) {
                        mediaView.requestFocus();
                    } else {
                        largeImageView.requestFocus();
                    }
                }
                e.consume();
            }
        };

        largeImageView.addEventHandler(KeyEvent.KEY_PRESSED, handler);
        mediaView.addEventHandler(KeyEvent.KEY_PRESSED, handler);

        GridPane imageList = new GridPane();
        imageList.setVgap(4);
        imageList.setHgap(4);
        imageList.setAlignment(Pos.TOP_CENTER);
        imageList.prefWidthProperty().bind(stage.widthProperty());
        imageList.prefHeightProperty().bind(stage.heightProperty().multiply(0.8));
        for (int x = 0; x < 8; x++) {
            ColumnConstraints col = new ColumnConstraints();
            col.prefWidthProperty().bind(imageList.prefWidthProperty().divide(8).subtract(8));
            imageList.getColumnConstraints().add(col);
            for (int y = 0; y < 4; y++) {
                RowConstraints row = new RowConstraints();
                row.prefHeightProperty().bind(imageList.prefHeightProperty().divide(4).subtract(8));
                imageList.getRowConstraints().add(row);

                Rectangle box = new Rectangle();
                box.setFill(Color.LIGHTGRAY);
                box.widthProperty().bind(imageList.prefWidthProperty().divide(8).subtract(8));
                box.heightProperty().bind(imageList.prefHeightProperty().divide(4).subtract(8));
                imageList.add(box, x, y);

                Text loading = new Text("Loading...");
                GridPane.setHalignment(loading, HPos.CENTER);
                GridPane.setValignment(loading, VPos.CENTER);
                loading.setFill(Color.BLACK);
                ImageView view = new ImageView();
                view.fitWidthProperty().bind(imageList.prefWidthProperty().divide(8).subtract(8));
                view.fitHeightProperty().bind(imageList.prefHeightProperty().divide(4).subtract(8));
                GridPane.setHalignment(view, HPos.CENTER);
                GridPane.setValignment(view, VPos.CENTER);
                view.setPreserveRatio(true);
                view.setVisible(false);
                view.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            if (view.isVisible()) {
                                if (showAlbum == null) { albumIndex = 0; }
                                if (mediaView.getMediaPlayer() != null) {
                                    MediaPlayer mp = mediaView.getMediaPlayer();
                                    mediaView.setMediaPlayer(null);
                                    mp.dispose();
                                }
                                largeImagePane.setCenter(largeImageView);
                                largeImagePane.setVisible(true);
                                largeImageView.setImage(view.getImage());
                                largeImageView.requestFocus();
                                try {
                                    index = Integer.parseInt(view.getId());
                                    indexLabel.setText("Image #" + Integer.toString(index));
                                    if (showAlbum == null) {
                                        nameLabel.setText(results.get(index).getName());
                                        if (results.get(index).isAlbum()) {
                                            Album album = (Album) results.get(index);
                                            albumIndexLabel.setText(String.format("Page %d/%d", 1, album.getImageCount()));
                                            if (album.getMetadata().getThumbnailID() > 0) {
                                                largeImageView.setImage(album.getImage(0).getImage());
                                            }
                                        } else if (((Image) results.get(index)).isGif()) {
                                            largeImageView.setImage(results.get(index).getImage());
                                            albumIndexLabel.setText("");
                                        } else if (((Image) results.get(index)).isVideo()) {
                                            largeImagePane.setCenter(mediaView);
                                            mediaView.requestFocus();
                                            mediaView.setMediaPlayer(((Image) results.get(index)).getMediaPlayerRepeat());
                                        } else {
                                            albumIndexLabel.setText("");
                                        }
                                    } else {
                                        nameLabel.setText(showAlbum.getImage(index).getName());
                                        if (showAlbum.getImage(index).isGif()) {
                                            largeImageView.setImage(showAlbum.getImage(index).getImage());
                                            albumIndexLabel.setText("");
                                        } else {
                                            albumIndexLabel.setText("");
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("Failed to get image index");
                                }
                            }
                        }
                    }
                });
                loading.visibleProperty().bind(view.visibleProperty().not());
                imageList.add(loading, x, y);
                imageList.add(view, x, y);

                Label imageCount = new Label();
                GridPane.setHalignment(imageCount, HPos.RIGHT);
                GridPane.setValignment(imageCount, VPos.BOTTOM);
                imageCount.setFont(new Font("Arial", 20));
                imageCount.setPadding(new Insets(0, 4, 0, 4));
                imageCount.setTextFill(Color.WHITE);
                imageCount.setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, 0.75), null, null)));
                imageCount.setVisible(false);
                imageList.add(imageCount, x, y);
            }
        }

        Button prevPage = new Button("<<");
        Label label = new Label("Page 1/1");
        label.setId("pagenum");
        Button nextPage = new Button(">>");

        prevPage.setFont(new Font("Arial", 20));
        label.setFont(new Font("Arial", 20));
        nextPage.setFont(new Font("Arial", 20));

        prevPage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                prevPage(imageList, label);
            }
        });
        nextPage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                nextPage(imageList, label);
            }
        });

        prevPage.setAlignment(Pos.CENTER);
        label.setAlignment(Pos.CENTER);
        nextPage.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(0, 12, 6, 12));

        TextField searchBar = new TextField();
        searchBar.setFont(new Font("Arial", 16));
        searchBar.setPromptText("Search");
        HBox.setMargin(searchBar, new Insets(0, 0, 2, 30));

        Button searchButton = new Button("GO");
        searchButton.setFont(new Font("Arial", 16));
        HBox.setMargin(searchButton, new Insets(0, 0, 2, 8));
        
        Label resultCount = new Label("Showing all " + Integer.toString(results.size()) + " images");
        resultCount.setFont(new Font("Arial", 16));

        searchButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                search(imageList, label, searchBar);
                if (searchBar.getText().equals("")) {
                    resultCount.setText("Showing all " + Integer.toString(results.size()) + " images");
                } else {
                    resultCount.setText("Found " + Integer.toString(results.size()) + " results");
                }
            }
        });

        searchBar.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    search(imageList, label, searchBar);
                    if (searchBar.getText().equals("")) {
                        resultCount.setText("Showing all " + Integer.toString(results.size()) + " images");
                    } else {
                        resultCount.setText("Found " + Integer.toString(results.size()) + " results");
                    }
                }
            }
        });

        Label speedText = new Label("Frame Duration");
        speedText.setFont(new Font("Arial", 16));
        TextField speed = new TextField();
        speed.setText("3");
        speed.setPrefWidth(40);
        speed.setFont(new Font("Arial", 16));
        HBox.setMargin(speed, new Insets(0, 8, 2, 0));
        speed.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*")) {
                    newValue = newValue.replaceAll("[^\\d]", "");
                }
                if (newValue.length() > 2) {
                    newValue = newValue.substring(0, 2);
                }
                if (newValue.length() <= 0 || newValue.equals("0") || newValue.equals("00")) {
                    newValue = "1";
                }
                speed.setText(newValue);
            }
        });

        Button slideshow = new Button("Slideshow");
        slideshow.setPrefWidth(140);
        slideshow.setFont(new Font("Arial", 16));
        HBox.setMargin(slideshow, new Insets(0, 8, 2, 0));

        Button shuffle = new Button("Shuffle");
        shuffle.setPrefWidth(140);
        shuffle.setFont(new Font("Arial", 16));
        HBox.setMargin(shuffle, new Insets(0, 30, 2, 0));
        
        CheckBox fullscreen = new CheckBox("Fullscreen");
        fullscreen.setSelected(true);
        fullscreen.setFont(new Font("Arial", 16));

        FlowPane slideshows = new FlowPane(slideshow, shuffle, speedText, speed, fullscreen);
        slideshows.setAlignment(Pos.CENTER);
        slideshows.setHgap(10);
        slideshows.setVgap(4);
        slideshows.setPrefWidth(300);

        FlowPane searching = new FlowPane(searchBar, searchButton, resultCount);
        searching.setAlignment(Pos.CENTER);
        searching.setHgap(10);
        searching.setVgap(4);
        searching.setPrefWidth(300);

        HBox menu = new HBox(slideshows, prevPage, label, nextPage, searching);
        menu.setPadding(new Insets(0, 0, 0, 0));
        menu.setAlignment(Pos.CENTER);
        menu.prefWidthProperty().bind(stage.widthProperty());
        menu.prefHeightProperty().bind(stage.heightProperty().multiply(0.1));

        largeImageOverlay.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                largeImagePane.setVisible(false);
                newPage = Math.floorDiv(index, 32);
                if (newPage != page) {
                    page = newPage;
                    showPage(imageList, label);
                }
            }
        });
        largeImageView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (e.getButton() == MouseButton.SECONDARY || showAlbum != null) {
                    largeImagePane.setVisible(false);
                    newPage = Math.floorDiv(index, 32);
                    if (newPage != page) {
                        page = newPage;
                        showPage(imageList, label);
                    }
                } else {
                    if (results.get(index).isAlbum()) {
                        Album album = (Album) results.get(index);
                        if (albumIndex < album.getImageCount() - 1) {
                            albumIndex++;
                            largeImageView.setImage(album.getImage(albumIndex).getImage());
                        } else {
                            index++;
                            albumIndex = 0;
                            largeImageView.setImage(results.get(index).getImage());
                        }
                    } else {
                        if (index < results.size() - 1) {
                            index++;
                            albumIndex = 0;
                            largeImageView.setImage(results.get(index).getImage());
                        }
                    }
                    indexLabel.setText("Image #" + Integer.toString(index));
                    nameLabel.setText(results.get(index).getName());
                    if (results.get(index).isAlbum()) {
                        Album album = (Album) results.get(index);
                        albumIndexLabel.setText(String.format("Page %d/%d", albumIndex + 1, album.getImageCount()));
                    } else {
                        albumIndexLabel.setText("");
                    }
                }
            }
        });

        Rectangle slideshowBackground = new Rectangle();
        slideshowBackground.setFill(Color.BLACK);
        slideshowBackground.widthProperty().bind(stage.widthProperty());
        slideshowBackground.heightProperty().bind(stage.heightProperty());
        ImageView slideshowImage = new ImageView();
        slideshowImage.fitWidthProperty().bind(stage.widthProperty());
        slideshowImage.fitHeightProperty().bind(stage.heightProperty());
        slideshowImage.setPreserveRatio(true);
        ImageView slideshowImage2 = new ImageView();
        slideshowImage2.fitWidthProperty().bind(stage.widthProperty());
        slideshowImage2.fitHeightProperty().bind(stage.heightProperty());
        slideshowImage2.setImage(viewables.get(5).getImage());
        slideshowImage2.setPreserveRatio(true);
        MediaView slideshowMedia = new MediaView(null);
        slideshowMedia.fitWidthProperty().bind(stage.widthProperty());
        slideshowMedia.fitHeightProperty().bind(stage.heightProperty());
        slideshowMedia.setPreserveRatio(true);
        slideshowMedia.setVisible(false);
        
        StackPane slideshowPane = new StackPane(slideshowBackground, slideshowImage, slideshowImage2, slideshowMedia);
        slideshowPane.setVisible(false);

        slideshow.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                slideshow(stage, slideshowPane, slideshowImage, slideshowImage2, slideshowMedia, speed, imageList, false, fullscreen.isSelected());
            }
        });
        shuffle.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                slideshow(stage, slideshowPane, slideshowImage, slideshowImage2, slideshowMedia, speed, imageList, true, fullscreen.isSelected());
            }
        });

        AnchorPane ui = new AnchorPane(imageList, menu);
        AnchorPane.setTopAnchor(imageList, 0.0);
        AnchorPane.setBottomAnchor(menu, 12.0);

        StackPane root = new StackPane(ui, largeImagePane, slideshowPane);

        stage.setTitle("Image Viewer");
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(1);
            }
        });

        // Create Context Menu
        // Album Metadata
        Menu albumMetaMenu = new Menu("Set Album Metadata...");
        MenuItem albumMetaItem1 = new MenuItem("For New Albums");
        MenuItem albumMetaItem2 = new MenuItem("For All Albums");
        albumMetaMenu.getItems().addAll(albumMetaItem1, albumMetaItem2);
        // Load Tags
        MenuItem tagsItem = new MenuItem("Load Tags from Files");
        // Close / Cancel
        MenuItem closeItem = new MenuItem("Cancel");
        // Actual Menu
        contextMenu.getItems().addAll(albumMetaMenu, tagsItem, closeItem);

        // Context Menu Actions
        albumMetaItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setAlbumMetadata(false, stage, ui, imageList, menu);
            }
        });
        albumMetaItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setAlbumMetadata(true, stage, ui, imageList, menu);
            }
        });
        
        tagsItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loadTagsFromFiles();
            }
        });

        menu.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                contextMenu.hide();
                contextMenu.show(root, event.getScreenX(), event.getScreenY());
            }
        });

        menu.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));

        stage.show();
        showPage(imageList, label);

    }

    public void loadTagsFromFiles() {

        System.out.println("Finding info files...");
        ArrayList<File> files = findInfoFiles(dir);

        System.out.printf("%d info files found.\n", files.size());

        int total = 0;

        ArrayList<Album> updateAlbums = new ArrayList<>();
        ArrayList<Image> updateImages = new ArrayList<>();

        for (Viewable v : viewables) {
            for (File f : files) {
                if (f.getName().substring(0, f.getName().lastIndexOf(".")).equals(v.getName())) {
                    try {
                        v.getMetadata().loadTags(f);
                        System.out.printf("%s: %s\n", v.getName(), v.getMetadata().getTags().toString());
                        total++;
                        if (v.isAlbum()) {
                            updateAlbums.add((Album) v);
                        } else {
                            updateImages.add((Image) v);
                        }
                        break;
                    } catch (Exception e) {
                        System.out.println("ERROR: Something went wrong when reading file.");
                    }
                    break;
                }
            }
        }

        Element root = (Element) metadata.getElementsByTagName("albums").item(0);
        NodeList nodes = root.getElementsByTagName("album");

        for (Album album : updateAlbums) {

            boolean found = false;

            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String name = node.getElementsByTagName("name").item(0).getTextContent();
                if (name.equals(album.getName())) {
                    if (node.getElementsByTagName("tags").getLength() > 0) {
                        Element tags = (Element) node.getElementsByTagName("tags").item(0);
                        while (tags.hasChildNodes()) {
                            tags.removeChild(tags.getFirstChild());
                        }
                        for (String s : album.getMetadata().getTags()) {
                            Element tag = metadata.createElement("tag");
                            tag.setTextContent(s);
                            tags.appendChild(tag);
                        }
                    } else if (album.getMetadata().getTags().size() > 0) {
                        ArrayList<String> tags = album.getMetadata().getTags();
                        if (tags.size() > 0) {
                            Element tagList = metadata.createElement("tags");
                            for (String s : tags) {
                                Element tag = metadata.createElement("tag");
                                tag.setTextContent(s);
                                tagList.appendChild(tag);
                            }
                            node.appendChild(tagList);
                        }
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                Element node = metadata.createElement("album");
                Element name = metadata.createElement("name");
                name.setTextContent(album.getName());
                node.appendChild(name);
                ArrayList<String> tagList = album.getMetadata().getTags();
                if (tagList.size() > 0) {
                    Element tags = metadata.createElement("tags");
                    for (String s : tagList) {
                        Element tag = metadata.createElement("tag");
                        tag.setTextContent(s);
                        tags.appendChild(tag);
                    }
                    node.appendChild(tags);
                }

                root.appendChild(node);
            }
        }

        root = (Element) metadata.getElementsByTagName("images").item(0);
        nodes = root.getElementsByTagName("image");

        for (Image image : updateImages) {

            boolean found = false;

            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String name = node.getElementsByTagName("name").item(0).getTextContent();
                if (name.equals(image.getName())) {
                    if (node.getElementsByTagName("tags").getLength() > 0) {
                        Element tags = (Element) node.getElementsByTagName("tags").item(0);
                        while (tags.hasChildNodes()) {
                            tags.removeChild(tags.getFirstChild());
                        }
                        for (String s : image.getMetadata().getTags()) {
                            Element tag = metadata.createElement("tag");
                            tag.setTextContent(s);
                            tags.appendChild(tag);
                        }
                    } else if (image.getMetadata().getTags().size() > 0) {
                        ArrayList<String> tags = image.getMetadata().getTags();
                        if (tags.size() > 0) {
                            Element tagList = metadata.createElement("tags");
                            for (String s : tags) {
                                Element tag = metadata.createElement("tag");
                                tag.setTextContent(s);
                                tagList.appendChild(tag);
                            }
                            node.appendChild(tagList);
                        }
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                Element node = metadata.createElement("image");
                Element name = metadata.createElement("name");
                name.setTextContent(image.getName());
                node.appendChild(name);
                ArrayList<String> tagList = image.getMetadata().getTags();
                if (tagList.size() > 0) {
                    Element tags = metadata.createElement("tags");
                    for (String s : tagList) {
                        Element tag = metadata.createElement("tag");
                        tag.setTextContent(s);
                        tags.appendChild(tag);
                    }
                    node.appendChild(tags);
                }

                root.appendChild(node);
            }
        }

        saveMetadata();

        System.out.printf("Updated tags for %d images.\n", total);

    }

    public ArrayList<File> findInfoFiles(File dir) {

        ArrayList<File> files = new ArrayList<>();

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                files.addAll(findInfoFiles(f));
            } else if (dir.getAbsolutePath().contains("\\Info")) {
                if (f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf(".")).equals(".txt")) {
                    files.add(f);
                }
            }
        }

        return files;

    }

    public void setAlbumMetadata(boolean all, Stage stage, AnchorPane ui, GridPane imageList, HBox menu) {

        ArrayList<Album> editAlbums = new ArrayList<>();
        if (all) {
            editAlbums.addAll(albums);
        } else {
            for (Album a : albums) {
                if (!a.getMetadata().isFromXML()) {
                    editAlbums.add(a);
                }
            }
        }

        page = 0;
        index = 0;
        albumIndex = 0;
        showAlbum = editAlbums.get(albumIndex);

        Button prevPage = new Button("<<");
        Label label = new Label(String.format("Page 1/%d", Math.floorDiv(showAlbum.size(), 32) + 1));
        Button nextPage = new Button(">>");

        prevPage.setFont(new Font("Arial", 20));
        label.setFont(new Font("Arial", 20));
        nextPage.setFont(new Font("Arial", 20));

        prevPage.setAlignment(Pos.CENTER);
        label.setAlignment(Pos.CENTER);
        nextPage.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(0, 12, 6, 12));

        prevPage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (page > 0) {
                    page--;
                    label.setText(String.format("Page %d/%d", page + 1, Math.floorDiv(showAlbum.size(), 32) + 1));
                    showAlbumImages(imageList, showAlbum);
                }
            }
        });
        nextPage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (page < Math.floorDiv(showAlbum.size(), 32)) {
                    page++;
                    label.setText(String.format("Page %d/%d", page + 1, Math.floorDiv(showAlbum.size(), 32) + 1));
                    showAlbumImages(imageList, showAlbum);
                }
            }
        });

        Button button = new Button("Hide Overlay");
        button.setPrefWidth(130);
        button.setStyle("-fx-font: 16px \"Arial\";");

        button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (button.getText().equals("Hide Overlay")) {
                    button.setText("Show Overlay");
                } else {
                    button.setText("Hide Overlay");
                }
            }
        });

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                final int id = x + (y * 8);

                ImageView imageView = null;
                for (javafx.scene.Node n : imageList.getChildren()) {
                    int x2 = GridPane.getColumnIndex(n);
                    int y2 = GridPane.getRowIndex(n);
                    if (n instanceof ImageView) {
                        if (x == x2 && y == y2) {
                            imageView = (ImageView) n;
                            break;
                        }
                    }
                }
                final ImageView imageViewFinal = imageView;

                Rectangle rect = new Rectangle();
                rect.setFill(new Color(0, 0, 0, 0.6));
                rect.widthProperty().bind(imageList.prefWidthProperty().divide(8).subtract(8));
                rect.heightProperty().bind(imageList.prefHeightProperty().divide(4).subtract(8));
                rect.setVisible(false);

                CheckBox showBox = new CheckBox();
                showBox.setPadding(new Insets(6, 6, 6, 6));
                showBox.setSelected(true);
                showBox.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (showBox.isSelected()) {
                            showAlbum.getMetadata().removeHideImage(page * 32 + id);
                        } else {
                            showAlbum.getMetadata().addHideImage(page * 32 + id);
                        }
                        showAlbum.getMetadata().sortHideImages();
                    }
                });
                showBox.setId("show");

                rect.visibleProperty().bind(showBox.selectedProperty().not());

                Button setThumbnail = new Button("Set Thumbnail");
                setThumbnail.setFont(new Font("Arial", 12));
                setThumbnail.setPadding(new Insets(4, 4, 4, 4));
                setThumbnail.setId("thumbnail");
                setThumbnail.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        for (javafx.scene.Node n : imageList.lookupAll("#thumbnail")) {
                            Button button = (Button) n;
                            button.setText("Set as Thumbnail");
                        }
                        setThumbnail.setText("Current Thumbnail");
                        showAlbum.getMetadata().setThumbnailID(page * 32 + id);
                    }
                });

                AnchorPane pane = new AnchorPane(rect, showBox, setThumbnail);
                AnchorPane.setTopAnchor(showBox, 0.0);
                AnchorPane.setBottomAnchor(setThumbnail, 0.0);
                pane.prefWidthProperty().bind(imageList.prefWidthProperty().divide(8).subtract(8));
                pane.prefHeightProperty().bind(imageList.prefHeightProperty().divide(4).subtract(8));
                pane.visibleProperty().bind(button.textProperty().isEqualTo("Hide Overlay").and(imageViewFinal.visibleProperty()));
                imageList.add(pane, x, y);
            }
        }

        
        String[] items = {"Album", "Collection", "Doujin", "Sequence", "Exclude"};
        ChoiceBox<String> typeInput = new ChoiceBox<>();
        typeInput.getItems().addAll(items);
        typeInput.setValue(items[showAlbum.getMetadata().getType()]);
        typeInput.setStyle("-fx-font: 16px \"Arial\";");
        typeInput.setPrefWidth(120);
        typeInput.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                showAlbum.getMetadata().setType(newValue.shortValue());
            }
        });

        Button saveButton = new Button("Save and Continue");
        saveButton.setStyle("-fx-font: 16px \"Arial\";");

        Button skipButton = new Button("Skip");
        skipButton.setStyle("-fx-font: 16px \"Arial\";");

        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-font: 16px \"Arial\";");

        HBox metaMenu = new HBox(typeInput, button, prevPage, label, nextPage, saveButton, skipButton, exitButton);
        metaMenu.setPadding(new Insets(0, 0, 12, 0));
        metaMenu.setAlignment(Pos.CENTER);
        metaMenu.prefWidthProperty().bind(stage.widthProperty());
        metaMenu.prefHeightProperty().bind(stage.heightProperty().multiply(0.1));

        saveButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                saveAlbumMetadata(showAlbum);
                albumIndex++;
                page = 0;
                if (albumIndex >= editAlbums.size()) {
                    hideAlbumMetadataMenu(ui, imageList, menu, metaMenu);
                } else {
                    showAlbum = editAlbums.get(albumIndex);
                    label.setText(String.format("Page 1/%d", Math.floorDiv(showAlbum.size(), 32) + 1));
                    typeInput.setValue(items[showAlbum.getMetadata().getType()]);
                    showAlbumImages(imageList, showAlbum);
                }
            }
        });

        skipButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                albumIndex++;
                page = 0;
                if (albumIndex >= editAlbums.size()) {
                    hideAlbumMetadataMenu(ui, imageList, menu, metaMenu);
                } else {
                    showAlbum = editAlbums.get(albumIndex);
                    label.setText(String.format("Page 1/%d", Math.floorDiv(showAlbum.size(), 32) + 1));
                    typeInput.setValue(items[showAlbum.getMetadata().getType()]);
                    showAlbumImages(imageList, showAlbum);
                }
            }
        });

        exitButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                hideAlbumMetadataMenu(ui, imageList, menu, metaMenu);
            }
        });

        ui.getChildren().remove(menu);
        ui.getChildren().add(metaMenu);
        AnchorPane.setBottomAnchor(metaMenu, 12.0);

        showAlbumImages(imageList, showAlbum);

    }

    public void hideAlbumMetadataMenu(AnchorPane ui, GridPane list, HBox menu, HBox submenu) {
        page = 0;
        index = 0;
        albumIndex = 0;
        showAlbum = null;
        ui.getChildren().remove(submenu);
        ui.getChildren().add(menu);
        AnchorPane.setBottomAnchor(menu, 12.0);


        for (int i = list.getChildren().size() - 1; i >= 0; i--) {
            javafx.scene.Node n = list.getChildren().get(i);
            if (n instanceof AnchorPane) {
                list.getChildren().remove(n);
            }
        }

        showPage(list, (Label) menu.lookup("#pagenum"));
    }

    public void saveAlbumMetadata(Album album) {
        album.getMetadata().setFromXML();
        Element albums = (Element) metadata.getElementsByTagName("albums").item(0);
        NodeList nodes = albums.getElementsByTagName("album");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            String name = node.getElementsByTagName("name").item(0).getTextContent();
            if (name.equals(album.getName())) {
                if (node.getElementsByTagName("type").getLength() > 0) {
                    Element type = (Element) node.getElementsByTagName("type").item(0);
                    type.setTextContent(Short.toString(album.getMetadata().getType()));
                } else {
                    Element type = metadata.createElement("type");
                    type.setTextContent(Short.toString(album.getMetadata().getType()));
                    node.appendChild(type);
                }
                if (node.getElementsByTagName("thumb").getLength() > 0) {
                    Element thumb = (Element) node.getElementsByTagName("thumb").item(0);
                    thumb.setTextContent(Integer.toString(album.getMetadata().getThumbnailID()));
                } else if (album.getMetadata().getThumbnailID() > 0) {
                    Element thumb = metadata.createElement("thumb");
                    thumb.setTextContent(Integer.toString(album.getMetadata().getThumbnailID()));
                    node.appendChild(thumb);
                }
                if (node.getElementsByTagName("hide").getLength() > 0) {
                    Element hide = (Element) node.getElementsByTagName("hide").item(0);
                    while (hide.hasChildNodes()) {
                        hide.removeChild(hide.getFirstChild());
                    }
                    for (Integer j : album.getMetadata().getHideImageIDs()) {
                        Element hideID = metadata.createElement("id");
                        hideID.setTextContent(j.toString());
                        hide.appendChild(hideID);
                    }
                } else if (album.getMetadata().getHideImageIDs().size() > 0) {
                    ArrayList<Integer> hideImages = album.getMetadata().getHideImageIDs();
                    if (hideImages.size() > 0) {
                        Element hide = metadata.createElement("hide");
                        for (Integer j : hideImages) {
                            Element hideID = metadata.createElement("id");
                            hideID.setTextContent(j.toString());
                            hide.appendChild(hideID);
                        }
                        node.appendChild(hide);
                    }
                }

                System.out.printf("Updated tags for %s\n", name);
                saveMetadata();
                return;
            }
        }
        Element node = metadata.createElement("album");
        Element name = metadata.createElement("name");
        name.setTextContent(album.getName());
        node.appendChild(name);
        Element type = metadata.createElement("type");
        type.setTextContent(Short.toString(album.getMetadata().getType()));
        node.appendChild(type);
        if (album.getMetadata().getThumbnailID() > 0) {
            Element thumb = metadata.createElement("thumb");
            thumb.setTextContent(Integer.toString(album.getMetadata().getThumbnailID()));
            node.appendChild(thumb);
        }
        ArrayList<Integer> hideImages = album.getMetadata().getHideImageIDs();
        if (hideImages.size() > 0) {
            Element hide = metadata.createElement("hide");
            for (Integer i : hideImages) {
                Element hideID = metadata.createElement("id");
                hideID.setTextContent(i.toString());
                hide.appendChild(hideID);
            }
            node.appendChild(hide);
        }

        albums.appendChild(node);
        System.out.printf("Added tags for %s\n", album.getName());
        saveMetadata();
    }

    public void showAlbumImages(GridPane list, Album album) {

        ArrayList<ImageView> updateViews = new ArrayList<>();
        ArrayList<Viewable> updateImages = new ArrayList<>();

        for (javafx.scene.Node n : list.getChildren()) {
            if (n instanceof ImageView) {
                ImageView image = (ImageView) n;
                int x = GridPane.getColumnIndex(n);
                int y = GridPane.getRowIndex(n);
                int id = page * 32 + x + (y * 8);
                Label label = null;
                AnchorPane overlay = null;
                for (javafx.scene.Node n2 : list.getChildren()) {
                    int x2 = GridPane.getColumnIndex(n2);
                    int y2 = GridPane.getRowIndex(n2);
                    if (n2 instanceof Label) {
                        if (x == x2 && y == y2) {
                            label = (Label) n2;
                        }
                    } else if (n2 instanceof AnchorPane) {
                        if (x == x2 && y == y2) {
                            overlay = (AnchorPane) n2;
                        }
                    }
                    if (label != null && overlay != null) {
                        break;
                    }
                }
                CheckBox box = (CheckBox) overlay.lookup("#show");
                if (album.getMetadata().isImageHidden(id)) {
                    box.setSelected(false);
                } else {
                    box.setSelected(true);
                }
                Button thumbnail = (Button) overlay.lookup("#thumbnail");
                if (album.getMetadata().getThumbnailID() == id) {
                    thumbnail.setText("Current Thumbnail");
                } else {
                    thumbnail.setText("Set as Thumbnail");
                }
                if (id < album.size()) {
                    if (fullLoad) {
                        image.setImage(album.getImage(id).getImage());
                        image.setVisible(true);
                    } else {
                        updateViews.add(image);
                        updateImages.add(album.getImage(id));
                        image.setVisible(false);
                    }
                    Image viewable = album.getImage(id);
                    if (viewable.isGif()) {
                        label.setText("GIF");
                        label.setVisible(true);
                    } else {
                        label.setVisible(false);
                    }
                } else {
                    image.setVisible(false);
                    label.setVisible(false);
                }
            }
        }

        if (updateViews.size() > 0 && !fullLoad) {
            for (int i = 0; i < updateViews.size(); i++) {
                LoadPageTask task = new LoadPageTask(updateViews.get(i), updateImages.get(i));
                task.setOnSucceeded(e -> {
                    task.getView().setImage(task.getResult());
                    task.getView().setVisible(true);
                    task.getView().setId(Integer.toString(album.getImages().indexOf(task.getImage())));
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        }

    }

    public void search(GridPane list, Label pageLabel, TextField search) {

        String term = search.getText().toLowerCase();

        page = 0;
        index = 0;

        results.clear();
        if (term.length() <= 0) {
            results.addAll(viewables);
        } else {
            String[] terms = term.split(" ");
            for (Viewable v : viewables) {
                boolean add = true;
                boolean orSearch = false;
                boolean orMatch = false;
                for (String s : terms) {
                    if (s.length() > 0) {
                        if (s.charAt(0) == '-') { // Exclude
                            s = s.substring(1);
                            if (v.matchCriteria(s)) {
                                add = false;
                                break;
                            }
                        } else if (s.charAt(0) == '+') { // Include
                            s = s.substring(1);
                            orSearch = true;
                            if (v.matchCriteria(s)) {
                                orMatch = true;
                            }
                        } else { // Require
                            if (!v.matchCriteria(s)) {
                                add = false;
                                break;
                            }
                        }
                    }
                }
                if (add && (!orSearch || orMatch)) {
                    results.add(v);
                }
            }
        }
        showPage(list, pageLabel);

    }

    public void prevPage(GridPane list, Label label) {
        if (page > 0) {
            page--;
        } else {
            page = getPageCount() - 1;
        }
        showPage(list, label);
    }

    public void nextPage(GridPane list, Label label) {
        if (page < getPageCount() - 1) {
            page++;
        } else {
            page = 0;
        }
        showPage(list, label);
    }

    public int getPageCount() {
        int pages = Math.floorDiv(results.size(), 32);
        if (results.size() % 32 != 0) { pages++; }
        return pages;
    }

    public void showPage(GridPane list, Label pageLabel) {

        ArrayList<ImageView> updateViews = new ArrayList<>();
        ArrayList<Viewable> updateImages = new ArrayList<>();

        pageLabel.setText(String.format("Page %d/%d", page + 1, getPageCount()));

        for (javafx.scene.Node n : list.getChildren()) {
            if (n instanceof ImageView) {
                ImageView image = (ImageView) n;
                int x = GridPane.getColumnIndex(n);
                int y = GridPane.getRowIndex(n);
                int id = page * 32 + x + (y * 8);
                Label label = null;
                for (javafx.scene.Node n2 : list.getChildren()) {
                    if (n2 instanceof Label) {
                        int x2 = GridPane.getColumnIndex(n2);
                        int y2 = GridPane.getRowIndex(n2);
                        if (x == x2 && y == y2) {
                            label = (Label) n2;
                            break;
                        }
                    }
                }
                if (id < results.size()) {
                    if (fullLoad) {
                        image.setImage(results.get(id).getImage());
                        image.setVisible(true);
                    } else {
                        updateViews.add(image);
                        updateImages.add(results.get(id));
                        image.setVisible(false);
                    }
                    if (results.get(id).isAlbum()) {
                        label.setText(Integer.toString(results.get(id).getImageCount()));
                        label.setVisible(true);
                    } else {
                        Image viewable = (Image) results.get(id);
                        if (viewable.isGif()) {
                            label.setText("GIF");
                            label.setVisible(true);
                        } else {
                            label.setVisible(false);
                        }
                    }
                } else {
                    image.setImage(null);
                    image.setVisible(false);
                    label.setVisible(false);
                }
            }
        }

        if (updateViews.size() > 0 && !fullLoad) {
            for (int i = 0; i < updateViews.size(); i++) {
                LoadPageTask task = new LoadPageTask(updateViews.get(i), updateImages.get(i));
                task.setOnSucceeded(e -> {
                    task.getView().setImage(task.getResult());
                    task.getView().setVisible(true);
                    task.getView().setId(Integer.toString(results.indexOf(task.getImage())));
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        }

    }

    public void slideshow(Stage stage, StackPane slidePane, ImageView view, ImageView view2, MediaView mview, TextField speedField, GridPane imageList, boolean shuffle, boolean fullscreen) {

        imageList.setVisible(false);
        if (fullscreen) { stage.setFullScreen(true); }
        slidePane.setVisible(true);

        view2.translateYProperty().set(slidePane.getHeight());

        ArrayList<Viewable> slideshow = new ArrayList<>();
        slideshow.addAll(results);

        for (int i = slideshow.size() - 1; i >= 0; i--) {
            Viewable v = slideshow.get(i);
            if (v.isAlbum()) {
                Album a = (Album) v;
                if (a.getMetadata().isCollection()) {
                    slideshow.addAll(i, a.getCollectionImages());
                    slideshow.remove(v);
                } else if (a.getMetadata().isExcluded()) {
                    slideshow.remove(v);
                }
            }
        }

        if (shuffle) {
            Collections.shuffle(slideshow);
        }

        ArrayList<Integer> transitions = new ArrayList<>();
        for (int i = 0; i < slideshow.size(); i++) {
            transitions.add(0);
        }

        for (int i = slideshow.size() - 1; i >= 0; i--) {
            Viewable v = slideshow.get(i);
            if (v.isAlbum()) {
                Album a = (Album) v;
                if (a.getMetadata().isDoujin() || a.getMetadata().isSequence()) {
                    slideshow.addAll(i, a.getCollectionImages());
                    slideshow.remove(v);
                    transitions.remove(i);
                    for (int j = 1; j < a.getCollectionImages().size(); j++) {
                        if (a.getMetadata().isDoujin()) {
                            transitions.add(i, 1);
                        } else {
                            transitions.add(i, 2);
                        }
                    }
                    transitions.add(i, 0);
                }
            }
        }

        if (slideshow.size() != transitions.size()) {
            System.out.println("Transition size does not align, something went wrong");
        }

        int speed = 3;
        try {
            speed = Integer.parseInt(speedField.getText());
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Failed to get slideshow speed.");
        }

        albumIndex = 0;
        index = 0;
        view.setImage(slideshow.get(index).getImage());

        slideshowActive = true;

        slidePane.requestFocus();
        slidePane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    slideshowActive = false;
                    stage.setFullScreen(false);
                    if (currentTask != null) {
                        currentTask.finish();
                    }
                } else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.RIGHT) {
                    if (currentTask != null) {
                        currentTask.finish();
                    }
                } else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
                    if (currentTask != null) {
                        currentTask.finishOne();
                    }
                }
            }
        });
        slidePane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY) {
                    slideshowActive = false;
                    stage.setFullScreen(false);
                    if (currentTask != null) {
                        currentTask.finish();
                    }
                } else if (event.getButton() == MouseButton.PRIMARY) {
                    if (currentTask != null) {
                        currentTask.finish();
                    }
                } else if (event.getButton() == MouseButton.MIDDLE) {
                    if (currentTask != null) {
                        currentTask.finishOne();
                    }
                }
            }
        });

        Timer timer = new Timer();
        currentTask = new SlideshowTimerTask(slideshow, timer, stage, slidePane, view, view2, mview, speed, imageList, transitions);

        timer.schedule(currentTask, (fullscreen ? 6000 : (speed * 1000)));

    }

    private class SlideshowTimerTask extends TimerTask {

        private ArrayList<Viewable> images;
        private Timer timer;
        private Stage stage;
        private StackPane pane;
        private ImageView view;
        private ImageView view2;
        private MediaView mview;
        private int speed;
        private GridPane imageList;
        private ArrayList<Integer> transitions;
        private SlideshowTimerTask nextTask;
        private boolean skip;
        private javafx.scene.image.Image nextImage;

        public SlideshowTimerTask(ArrayList<Viewable> images, Timer timer, Stage stage, StackPane pane, ImageView view, ImageView view2, MediaView mview, int speed, GridPane imageList, ArrayList<Integer> transitions) {
            super();
            this.images = images;
            this.timer = timer;
            this.stage = stage;
            this.pane = pane;
            this.view = view;
            this.view2 = view2;
            this.mview = mview;
            this.speed = speed;
            this.imageList = imageList;
            this.transitions = transitions;
            this.skip = false;
            this.nextImage = null;
        }

        public SlideshowTimerTask(ArrayList<Viewable> images, Timer timer, Stage stage, StackPane pane, ImageView view, ImageView view2, MediaView mview, int speed, GridPane imageList, ArrayList<Integer> transitions, javafx.scene.image.Image nextImage) {
            super();
            this.images = images;
            this.timer = timer;
            this.stage = stage;
            this.pane = pane;
            this.view = view;
            this.view2 = view2;
            this.mview = mview;
            this.speed = speed;
            this.imageList = imageList;
            this.transitions = transitions;
            this.skip = false;
            this.nextImage = nextImage;
        }

        public void finishOne() {
            skip = true;
        }

        public void finish() {
            int nextIndex = index + 1;
            if (nextIndex > images.size()) {
                nextIndex = 0;
            }
            while (transitions.get(nextIndex) == 1) {
                index++;
                if (index > images.size()) {
                    index = 0;
                }
                nextIndex++;
                if (nextIndex > images.size()) {
                    nextIndex = 0;
                }
            }
            skip = true;
        }

        @Override
        public void run() {

            timer.purge();

            if (!slideshowActive) {
                pane.setVisible(false);
                imageList.setVisible(true);
                view.setOnKeyPressed(null);
                currentTask = null;
            } else {
                currentTask = this;
                index++;
                if (index >= images.size()) {
                    index = 0;
                }
                int transition = transitions.get(index);
                boolean spin = Math.random() > 0.99;
                boolean spin2 = Math.random() > 0.99;
                Viewable nextViewable = images.get(index);
                if (mview.getMediaPlayer() != null) {
                    MediaPlayer mp = mview.getMediaPlayer();
                    mview.setMediaPlayer(null);
                    mp.dispose();
                }
                if (!nextViewable.isAlbum() && ((Image) nextViewable).isVideo()) {
                    transition = -1;
                    view.setImage(null);
                    view.setVisible(false);
                    view2.setVisible(false);
                    mview.setVisible(true);
                    mview.setMediaPlayer(((Image) nextViewable).getMediaPlayer());
                } else {
                    view.setVisible(true);
                    view2.setVisible(true);
                    mview.setVisible(false);
                    if (nextImage == null) {
                        view2.setImage(images.get(index).getImage());
                    } else {
                        view2.setImage(nextImage);
                    }
                }

                final int nextIndex = ((index + 1) >= images.size() ? 0 : (index + 1));

                LoadImageRunnable nextImageRunnable = new LoadImageRunnable(images.get(nextIndex));
                Thread nextImageThread = new Thread(nextImageRunnable);
                nextImageThread.start();

                if (transition == 1) {
                    view.translateXProperty().set(0);
                    view.translateYProperty().set(0);
                    view2.translateXProperty().set(0);
                    view2.translateYProperty().set(pane.getHeight());
                    for (int i = speed * 4; i >= 0; i--) {
                        view2.translateYProperty().set(pane.getHeight() * i / (speed * 4));
                        sleep(1000 / 60);
                    }
                    for (int i = speed * 2; i >= 0; i--) {
                        view.opacityProperty().set((i * 1.0) / (speed * 2.0));
                        sleep(1000 / 60);
                    }
                    view.setImage(view2.getImage());
                    view2.setImage(null);
                    view.opacityProperty().set(1.0);
                    view.translateXProperty().set(0);
                    view.translateYProperty().set(0);
                } else if (transition == 2) {
                    view.translateXProperty().set(0);
                    view.translateYProperty().set(0);
                    view.opacityProperty().set(1.0);
                    view2.opacityProperty().set(0.0);
                    sleep(10);
                    view2.translateXProperty().set(0);
                    view2.translateYProperty().set(0);
                    for (int i = speed * 4; i >= 0; i--) {
                        view2.opacityProperty().set(1.0 - ((i * 1.0) / (speed * 4.0)));
                        sleep(1000 / 60);
                    }
                    view.setImage(view2.getImage());
                    view2.setImage(null);
                } else if (transition == 0) {
                    int time = speed;
                    if (spin || spin2) { time *= 3; };
                    view.translateXProperty().set(0);
                    view.translateYProperty().set(0);
                    view2.translateXProperty().set(pane.getWidth());
                    view2.translateYProperty().set(0);
                    for (int i = time * 6; i >= 0; i--) {
                        view2.translateXProperty().set(pane.getWidth() * i / (time * 6));
                        view.translateXProperty().set(view2.translateXProperty().get() - pane.getWidth());
                        if (spin) { view.rotateProperty().set(180 + 180 * i / (time * 6)); }
                        if (spin2) { view2.rotateProperty().set(180 * i / (time * 6)); }
                        sleep(1000 / 60);
                    }
                    view.rotateProperty().set(0);
                    view2.rotateProperty().set(0);
                    view.setImage(view2.getImage());
                    view2.setImage(null);
                    view.translateXProperty().set(0);
                    view.translateYProperty().set(0);
                }
                if (!nextViewable.isAlbum() && ((Image) nextViewable).isVideo()) {
                    BooleanObject done = new BooleanObject(false);
                    mview.getMediaPlayer().setOnEndOfMedia(() -> {
                        try {
                            nextImageThread.join();
                        } catch (InterruptedException e) {System.out.println("Interrupted Exception");}
                        nextTask = new SlideshowTimerTask(images, timer, stage, pane, view, view2, mview, speed, imageList, transitions, nextImageRunnable.getImage());
                        timer.schedule(nextTask, 0);
                        done.set(true);
                    });
                    while (!done.get() && !skip) {
                        sleep(50);
                    }
                    if (!done.get()) {
                        mview.getMediaPlayer().setOnEndOfMedia(null);
                        try {
                            nextImageThread.join();
                        } catch (InterruptedException e) {System.out.println("Interrupted Exception");}
                        nextTask = new SlideshowTimerTask(images, timer, stage, pane, view, view2, mview, speed, imageList, transitions, nextImageRunnable.getImage());
                        timer.schedule(nextTask, 0);
                    }
                } else if (!nextViewable.isAlbum() && ((Image) nextViewable).isGif()) {
                    long startTime = System.currentTimeMillis();
                    Image image = (Image) nextViewable;
                    GifDecoder d = gifDecoder;
                    d.read(image.getFile().getAbsolutePath());

                    long endTime = System.currentTimeMillis();

                    long time = 0;
                    for (int i = 0; i < d.getFrameCount(); i++) {
                        time += d.getDelay(i);
                    }

                    if (time < (speed * 1.5) * 1000) { time = (int) (speed * 1.5) * 1000; }
                    int totaltime = (int) (endTime - startTime);
                    while (time > totaltime && !skip) {
                        sleep(50);
                        totaltime += 50;
                    }
                    try {
                        nextImageThread.join();
                    } catch (InterruptedException e) {System.out.println("Interrupted Exception");}
                    nextImage = nextImageRunnable.getImage();
                    nextTask = new SlideshowTimerTask(images, timer, stage, pane, view, view2, mview, speed, imageList, transitions, nextImageRunnable.getImage());
                    timer.schedule(nextTask, 0);
                } else {
                    int time = speed * 1000;
                    if (transition == 2) { time *= 2; time /= 3; }
                    int totaltime = 0;
                    while (time > totaltime && !skip) {
                        sleep(50);
                        totaltime += 50;
                    }
                    try {
                        nextImageThread.join();
                    } catch (InterruptedException e) {System.out.println("Interrupted Exception");}
                    nextTask = new SlideshowTimerTask(images, timer, stage, pane, view, view2, mview, speed, imageList, transitions, nextImageRunnable.getImage());
                    timer.schedule(nextTask, 0);
                }
            }
        }

        private boolean sleep(int millis) {
            try {
                Thread.sleep(millis);
                return true;
            } catch (InterruptedException e) {
                System.out.println("interrupted");
                return false;
            }
        }
    }

    private class BooleanObject {

        private boolean value;

        public BooleanObject(boolean value) {
            this.value = value;
        }

        public void set(boolean value) {
            this.value = value;
        }

        public boolean get() {
            return value;
        }
    }

    private class LoadImageRunnable implements Runnable {

        private volatile javafx.scene.image.Image image;
        private Viewable viewable;

        public LoadImageRunnable(Viewable viewable) {
            super();
            this.viewable = viewable;
        }

        @Override
        public void run() {
            image = viewable.getImage();
        }

        public javafx.scene.image.Image getImage() {
            return image;
        }

    }
        

    private class LoadPageTask extends Task<Void> {

        private ImageView view;
        private Viewable image;
        private javafx.scene.image.Image result;

        public LoadPageTask(ImageView view, Viewable image) {
            this.view = view;
            this.image = image;
        }
        
        @Override
        protected Void call() throws InterruptedException {
            
            result = image.getThumbnail();

            return null;

        }

        public ImageView getView() {
            return view;
        }

        public Viewable getImage() {
            return image;
        }

        public javafx.scene.image.Image getResult() {
            return result;
        }
        
    }

}