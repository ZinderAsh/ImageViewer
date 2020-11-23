import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

class Metadata {

    protected Viewable parent;
    protected boolean fromXML;
    protected ArrayList<String> tags;

    public Metadata(Viewable parent) {
        this.parent = parent;
        this.fromXML = false;
        tags = new ArrayList<>();
    }

    public void loadTags(File file) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains(" ") && line.contains("=")) {
                    if (line.substring(0, line.indexOf(" ")).equals("Tags")) {
                        line = line.substring(line.indexOf("=") + 2);
                        String[] tags = line.split(", ");
                        for (String tag : tags) {
                            addTag(tag);
                        }
                        in.close();
                        return;
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            System.out.println("ERROR: Something went wrong when reading file");
        }
    }

    public void addTag(String tag) {
        if (!tags.contains(tag) && tag.replace(" ","").length() > 0) {
            tags.add(tag);
        }
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public boolean isFromXML() {
        return fromXML;
    }

    public void setFromXML() {
        fromXML = true;
    }

}