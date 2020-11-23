import java.util.ArrayList;
import java.util.Collections;

class MetadataAlbum extends Metadata {

    // 4 types:
    // 0: Album, displays one image, usually the first one
    // 1: Collection, will display all images unless specified otherwise
    // 2: Doujin, displays in a vertically scrolling view
    // 3: Succession, all images should be show in succession
    // 4: Excluded, do not show at all
    private short type;
    private int thumbnailID;
    private ArrayList<Integer> hideImageIDs;

    public MetadataAlbum(Album album) {
        super(album);
        this.type = 0;
        this.thumbnailID = 0;
        this.hideImageIDs = new ArrayList<>();
    }

    public Album getParent() {
        return (Album) parent;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getType() {
        return type;
    }

    public void setAlbum() {
        type = 0;
    }

    public boolean isAlbum() {
        return (type == 0);
    }

    public void setCollection() {
        type = 1;
    }

    public boolean isCollection() {
        return (type == 1);
    }

    public void setDoujin() {
        type = 2;
    }

    public boolean isDoujin() {
        return (type == 2);
    }

    public void setSequence() {
        type = 3;
    }

    public boolean isSequence() {
        return (type == 3);
    }

    public boolean isExcluded() {
        return (type == 4);
    }

    public void setThumbnailID(int id) {
        thumbnailID = id;
    }

    public int getThumbnailID() {
        return thumbnailID;
    }

    public void addHideImage(int id) {
        hideImageIDs.add(id);
    }

    public void addHideImage(Image image) {
        addHideImage(getParent().getImages().indexOf(image));
    }

    public void removeHideImage(int id) {
        if (hideImageIDs.contains(id)) {
            hideImageIDs.remove(new Integer(id));
        }
    }

    public void removeHideImage(Image image) {
        removeHideImage(getParent().getImages().indexOf(image));
    }

    public void sortHideImages() {
        Collections.sort(hideImageIDs);
    }

    public boolean isImageHidden(int id) {
        return hideImageIDs.contains(id);
    }

    public boolean isImageHidden(Image image) {
        return hideImageIDs.contains(getParent().getImages().indexOf(image));
    }

    public ArrayList<Integer> getHideImageIDs() {
        return hideImageIDs;
    }

}