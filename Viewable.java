public interface Viewable {

    abstract String getName();

    abstract javafx.scene.image.Image getImage();
    abstract javafx.scene.image.Image getThumbnail();

    abstract boolean isAlbum();
    
    abstract int getImageCount();

    abstract String getPath();

    abstract boolean matchCriteria(String search);

    abstract Metadata getMetadata();

}