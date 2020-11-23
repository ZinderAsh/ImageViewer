import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

class Image implements Viewable {

    private File file;
    private int page;
    private MetadataImage metadata;
    private static javafx.scene.image.Image videoThumb = new javafx.scene.image.Image(new File("videothumb.jpg").toURI().toString());

    public Image(File file) {
        this.file = file;
        this.page = 0;
        this.metadata = new MetadataImage(this);
    }

    public Image(File file, int page) {
        this.file = file;
        this.page = page;
        this.metadata = new MetadataImage(this);
    }

    public MetadataImage getMetadata() {
        return metadata;
    }

    public BufferedImage getBufferedImage() throws IOException {
        return ImageIO.read(file);
    }

    public javafx.scene.image.Image getImage() {
        return new javafx.scene.image.Image(getURI());
    }

    public Media getMedia() {
        return new Media(file.toURI().toString());
    }

    public MediaPlayer getMediaPlayerRepeat() {
        MediaPlayer player = new MediaPlayer(getMedia());
        player.setAutoPlay(true);
        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
        });
        return player;
    }

    public MediaPlayer getMediaPlayer() {
        MediaPlayer player = new MediaPlayer(getMedia());
        player.setAutoPlay(true);
        return player;
    }

    public javafx.scene.image.Image getThumbnail() {
        if (isVideo()) {
            return videoThumb;
        } else if (isGif()) {
            GifDecoder gifDecoder = new GifDecoder();
            gifDecoder.read(file.getAbsolutePath());
            BufferedImage frame = gifDecoder.getFrame(0);
            gifDecoder.disposeFrames();
            return SwingFXUtils.toFXImage(frame, null);
        } else {
            return new javafx.scene.image.Image(getURI());
        }
    }

    public String getName() {
        return file.getName().substring(0, file.getName().lastIndexOf("."));
    }

    public File getFile() {
        return file;
    }
    
    public String getPath() {
        return file.getAbsolutePath();
    }

    public String getURI() {
        return file.toURI().toString();
    }

    public int getPage() {
        return page;
    }

    public boolean isGif() {
        return (getExtension().equals("gif"));
    }

    public boolean isVideo() {
        return (getExtension().equals("mp4"));
    }

    public String getExtension() {
        return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(".") + 1).toLowerCase();
    }

    public static boolean isImageFile(File file) {

        String[] exts = {"png", "jpg", "jpeg", "gif"};

        String ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(".") + 1).toLowerCase();
        for (String s : exts) {
            if (s.equals(ext)) {
                return true;
            }
        }
        if (ext.equals("mp4")) {
            if (file.getParentFile().getName().toLowerCase().contains("imageviewer")) {
                return true;
            }
        }

        return false;
    }

    public boolean isAlbum() {
        return false;
    }

    public int getImageCount() {
        return 1;
    }

    public boolean matchCriteria(String search) {
        search = search.toLowerCase();

        if (getPath().toLowerCase().contains(search)) {
            return true; // Name match
        } else if (search.equals("doujin")) {
            return false; // Doujin search
        } else if (search.equals("sequence")) {
            return false; // Sequence search
        } else if (search.equals("album")) {
            return false; // Album search
        } else if (search.equals("animated")) {
            if (isGif() || isVideo()) {
                return true;
            }
        }

        for (String tag : metadata.getTags()) {
            if (tag.toLowerCase().contains(search)) {
                return true;
            }
        }

        return false;
    }

}