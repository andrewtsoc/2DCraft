import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Map {

    //preloaded images
    public static Image[] baseimages;
    private Tile[][] twodarray;
    //reminder to self: twodarray[row][column]
    private int h, w;

    public Map(int width) {
        //parse in images
        initialize();
        h = 256;
        w = width;
        //space created
        twodarray = new Tile[h][width];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                twodarray[i][j] = new Tile(0, baseimages[0], false);
            }
        }
        //stone and dirts
        //initial elevations
        int stoneend = (int) (Math.random() * 3) + h / 2;
        int dirtend = (int) (stoneend - (Math.random() * 2 + 3));
        //keeping track of changes
        int netchange = 0;
        char updown = 'n';

        //left to right, varying elevation
        for (int i = 0; i < width; i++) {
            //dirt and stone generation
            //weighted randomly increase or decrease
            double randchange = Math.random() * 10;
            int dirtchange = 0;
            int stonechange = 0;
            if (randchange > 8) {
                stonechange++;
                dirtchange++;
                netchange++;
                updown = 'd';
            } else if (randchange < 2) {
                stonechange--;
                dirtchange--;
                netchange--;
                updown = 'u';
            } else {
                //don't do anything
            }
            //adjusting for elevation
            double randhchange = Math.random() * netchange;
            if (randhchange > h / 8) {
                stonechange--;
                dirtchange--;
                netchange--;
            } else if (randhchange < -1 * h / 8) {
                stonechange++;
                dirtchange++;
                netchange++;
            } else {
                //do nothing
            }
            stoneend = stoneend + stonechange;
            dirtend = dirtend + dirtchange;
            for (int j = h - 1; j > stoneend; j--) {
                twodarray[j][i] = new Tile(3, baseimages[3]);
            }
            for (int j = stoneend; j > dirtend; j--) {
                try {
                    twodarray[j][i] = new Tile(1, baseimages[1]);
                } catch (Exception e) {
                    System.out.println("j" + j + "i" + i);
                }
            }
            //grass layer
            twodarray[dirtend][i] = new Tile(2, baseimages[2]);
            //bedrock layer
            twodarray[h - 1][i] = new Tile(6, baseimages[6]);
        }

        //getting rid of the weird 1block /dip things
        for (int i = 1; i < width - 1; i++) {
            int a = getSurface(i - 1);
            int b = getSurface(i);
            int c = getSurface(i + 1);
            if (b - a == 1 && b - c == 1) {
                twodarray[b - 1][i] = new Tile(2, baseimages[2]);
                twodarray[b][i] = new Tile(1, baseimages[1]);
            }
            if (b - a == -1 && b - c == -1) {
                twodarray[b + 1][i] = new Tile(2, baseimages[2]);
                twodarray[b][i] = new Tile(0, baseimages[0]);
            }
        }

        //forest generation
        //Given a map is of size "n", the best way to equally space a number of forests "m"
        //is to start them roughly at locations of n/(m+1)*index of forest
        //technically the center should be at that location, but as map size
        //increases, relative error goes down so its okay

        //determining initial values
        int forestnum = (int) (Math.random() * w / 250) + 8;                        //number of forests
        int[] foreststarts = new int[forestnum];                                //start location of forest
        int[] forestsize = new int[forestnum];                                //"area" of forest
        int[] treenum = new int[forestnum];                                    //number of trees in a forest

        foreststarts[0] = w / (forestnum + 1) + (int) (Math.random() * 5);            //first forest start location
        forestsize[0] = (int) (Math.random() * 20) + 30;                            //first forest size
        treenum[0] = forestsize[0] / 8 + (int) (Math.random() * 5) - 2;                //number of trees based on forest size

        for (int k = 1; k < forestnum; k++) {
            foreststarts[k] = (k + 1) * w / (forestnum + 1) + (int) (Math.random() * 30) - 15;
            forestsize[k] = (int) (Math.random() * 20) + 30;
            treenum[k] = forestsize[k] / 8 + (int) (Math.random() * 4) - 1;
        }

        //debugging
        //for(int i=0;i<forestnum;i++)
        //{
        //	System.out.println("Start"+foreststarts[i]+"size"+forestsize[i]+"num"+treenum[i]);
        //}

        //looping per forest, per tree
        for (int i = 0; i < forestnum; i++) {
            for (int j = 0; j < treenum[i]; j++) {

                //leaf generation parameterss
                int centerx = foreststarts[i] + j * 8 + (int) (Math.random() * 3);
                int surface = getSurface(centerx);
                int height = (int) (Math.random() * 3) + 4;
                int topy = surface - height;
                double leafradius = Math.random() * (height - 3) + 3;
                //actual generation in an area around the tree
                for (int x = centerx - 20; x < centerx + 21; x++) {
                    for (int y = topy - 20; y < topy + 2; y++) {
                        //base generation
                        double manhattan = Math.abs(x - centerx) + Math.abs(y - topy);
                        //randomized minowski difference
                        //http://en.wikipedia.org/wiki/Minkowski_distance
                        double minowski = Math.random() * 5 + 0.75;
                        double dist = Math.pow(Math.pow(Math.abs(x - centerx), minowski) + Math.pow(Math.abs(y - topy), minowski), 1 / minowski);
                        //if its less than the leafradius
                        if (manhattan < leafradius || dist < leafradius - 1) {
                            //if the block was previously air
                            if (!twodarray[y][x].isSolid()) {
                                twodarray[y][x] = new Tile(5, baseimages[5], false);
                            }
                        }
                    }
                }
                //log generation
                for (int k = 0; k < height; k++) {
                    twodarray[surface - k - 1][centerx] = new Tile(4, baseimages[4]);
                }
            }
        }

        //ore generation
        for (int i = 20; i < w - 19; i++) {
            for (int j = 20; j < h - 19; j++) {
                if (twodarray[j][i].getId() == 3) {
                    //value of the ore
                    double orevalue = Math.random() * 905 + 1;
                    //if orevalue is more than 900, generate some sort of ores
                    if (orevalue > 900) {
                        int oretype = (int) ((j - this.getSurface(i) + orevalue / 100) / 15) + 6;
                        if (oretype < 7) {
                            oretype = 7;
                        }
                        if (oretype > 12) {
                            oretype = 12;
                        }
                        //total size of vein
                        double veinsize = Math.random() * 3 + 20 / Math.pow(oretype, 1.5);

                        for (int x = i - (int) (Math.random() * 10); x < i + (int) (Math.random() * 11); x++) {

                            for (int y = j - (int) (Math.random() * 11); y < j + (int) (Math.random() * 11); y++) {
                                //making ore veins flatter than they are wide
                                double manhattan = 0.25 * Math.abs(x - i) + Math.abs(y - j);
                                if (manhattan < veinsize) {
                                    twodarray[y][x] = new Tile(oretype, baseimages[oretype]);
                                }
                            }
                        }

                    }
                }
            }
        }


    }

    public void initialize() {
        //finding out number of possible tiles
        File tilelist;
        try {
            tilelist = new File(getClass().getResource("resources/tiles.txt").toURI());
        } catch(URISyntaxException e) {
            tilelist = new File(getClass().getResource("resources/tiles.txt").getPath());
        }

        int tilelength = 0;
        Scanner scan;
        try {
            scan = new Scanner(tilelist);
            while (scan.hasNextLine()) {
                if (!(scan.nextLine()).isEmpty()) {
                    tilelength++;
                }
            }
            scan.close();

        } catch (FileNotFoundException e1) {
            System.out.println("Tile declarations not found");
        }
        //setting image array size to number of tiles
        baseimages = new Image[tilelength];
        for (int i = 0; i < tilelength; i++) {
            try {
                baseimages[i] = ImageIO.read(getClass().getResource("resources/" + i + ".png"));
            } catch (Exception e) {
                System.out.println("Image not found");
            }
        }

    }

    public void debugDraw() {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                System.out.print(" " + twodarray[i][j].getId());
            }
            System.out.println("");
        }
    }

    public void save(String fname) {
        File towrite = new File("resources/" + fname + ".txt");
        try {
            PrintWriter goin = new PrintWriter(towrite);
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    goin.print(twodarray[i][j].getId());
                }
                goin.println("");
            }
            goin.close();
        } catch (IOException e) {
        }

    }

    /**
     * Accessor for map array
     *
     * @return
     */
    public Tile[][] getMapArray() {
        return twodarray;
    }

    /**
     * Accessor for map height
     *
     * @return
     */
    public int getHeight() {
        return h;
    }

    /**
     * Accessor for map width
     *
     * @return
     */
    public int getWidth() {
        return w;
    }

    public Image getImage(int x, int y) {
        return twodarray[y][x].getDisplaypic();
    }

    public int getSurface(int x) {
        for (int i = 0; i < h - 1; i++) {
            if (twodarray[i][x].isSolid()) {
                return i;
            }
        }
        return h - 1;
    }

    public int removeBlock(int x, int y) {
        int i = -1;
        if (isValid(x, y)) {
            i = twodarray[y][x].getId();
            twodarray[y][x] = new Tile(0, baseimages[0], false);
        }
        return i;
    }

    public int getBlockType(int x, int y) {
        if (isValid(x, y))
            return twodarray[y][x].getId();
        else
            return -1;
    }

    public void placeBlock(int x, int y, int n, boolean solid) {
        if (isValid(x, y)) {
            twodarray[y][x] = new Tile(n, baseimages[n], solid);
        }
    }

    public boolean isSolid(int x, int y) {
        if (isValid(x, y))
            return twodarray[y][x].isSolid();
        else
            return true;

    }

    protected boolean isValid(int x, int y) {
        return !(x < 0 || x >= w || y < 0 || y >= h);
    }
}
