import java.io.File;
import java.util.ArrayList;

class Album implements Viewable {

    private File dir;
    private ArrayList<Image> images;
    private MetadataAlbum metadata;

    public Album(File dir) {
        this.dir = dir;
        this.images = new ArrayList<>();
        int page = 0;
        for (File f : dir.listFiles()) {
            if (!f.isDirectory()) {
                if (Image.isImageFile(f)) {
                    images.add(new Image(f, page));
                    page++;
                }
            }
        }
        this.metadata = new MetadataAlbum(this);
    }

    public void setMetadata(MetadataAlbum metadata) {
        this.metadata = metadata;
    }

    public MetadataAlbum getMetadata() {
        return metadata;
    }

    public File getDirectory() {
        return dir;
    }

    public String getPath() {
        return dir.getAbsolutePath();
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public Image getImage(int index) {
        return images.get(index);
    }

    public String getName() {
        return dir.getName();
    }

    public javafx.scene.image.Image getImage() {
        if (metadata.getThumbnailID() >= 0 && metadata.getThumbnailID() < images.size()) {
            return images.get(metadata.getThumbnailID()).getThumbnail();
        }
        return images.get(0).getThumbnail();
    }

    public javafx.scene.image.Image getThumbnail() {
        return getImage();
    }

    public int size() {
        return images.size();
    }

    public static boolean isAlbumDir(File dir) {

        String standardName = null;
        boolean isNumber = false;

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                return false;
            } else if (f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf(".")).equals(".mp4")) {
                return false;
            }
            String filename = f.getName().replace("_", " ").replace("-", " ");
            if (filename.contains(".")) {
                filename = filename.substring(0, filename.lastIndexOf("."));
            }
            String name = "";
            if (filename.contains(" ")) {
                name = filename.substring(0, filename.indexOf(" "));
            } else {
                name = filename;
            }
            try {
                Integer.parseInt(name);
                isNumber = true;
            } catch (NumberFormatException e) {
                if (isNumber) {
                    return false;
                }
            }
            if (standardName == null) {
                standardName = name;
            } else if (!isNumber && !name.equals(standardName)) {
                return false;
            }
        }

        return true;
    }

    public ArrayList<Image> getCollectionImages() {
        ArrayList<Image> collection = new ArrayList<>();
        collection.addAll(images);
        for (int i = collection.size(); i >= 0; i--) {
            if (metadata.isImageHidden(i)) {
                collection.remove(i);
            }
        }
        return collection;
    }

    public boolean isAlbum() {
        return true;
    }

    public int getImageCount() {
        return images.size();
    }

    public boolean matchCriteria(String search) {
        search = search.toLowerCase();

        if (getPath().toLowerCase().contains(search)) {
            return true; // Name match
        } else if (search.equals("doujin") && metadata.isDoujin()) {
            return true; // Doujin search
        } else if (search.equals("sequence") && metadata.isSequence()) {
            return true; // Sequence search
        } else if (search.equals("album") && metadata.isSequence()) {
            return true; // Album search
        } else if (search.equals("animated")) {
            if (images.get(0).isGif()) {
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