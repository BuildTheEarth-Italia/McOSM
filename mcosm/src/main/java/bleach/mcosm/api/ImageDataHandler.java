package bleach.mcosm.api;

import bleach.mcosm.OSMInstance;
import bleach.mcosm.api.ApiDataHandler.Projection;
import bleach.mcosm.struct.building.BuildingStruct;
import bleach.mcosm.utils.GeoPos;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockConcretePowder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class ImageDataHandler {

    public Projection proj;
    public List<JsonObject> ways = new ArrayList<>();
    public List<JsonObject> nodes = new ArrayList<>();
    public double minLat = Integer.MAX_VALUE;
    public double minLon = Integer.MAX_VALUE;
    public double maxLat = Integer.MIN_VALUE;
    public double maxLon = Integer.MIN_VALUE;
    private String data;

    public ImageDataHandler(String json, Projection proj) {
        this.data = json;
        this.proj = proj;

        PriorityQueue<Pair<Integer /* priority */, JsonObject>> tempWays = new PriorityQueue<>(
                (o1, o2) -> o2.getLeft().compareTo(o1.getLeft()));

        // TODO: Parse file data and add to tempWays

        while (!tempWays.isEmpty()) {
            ways.add(tempWays.poll().getRight());
        }
    }

    public void addToInstance(OSMInstance inst) {
        // Itera ogni forma
        for (JsonObject j : ways) {
            // Punti sotto forma di blocchi minecraft
            List<BlockPos> puntiDefinitivi = new ArrayList<>();

            // Ottiene array di punti
            JsonElement jgeom = j.get("geometry");
            if (jgeom != null) {
                // Per ogni punto nell'array
                for (JsonElement point : jgeom.getAsJsonArray()) {
                    if (point.isJsonNull()) continue;

                    JsonObject pointObject = point.getAsJsonObject();

                    puntiDefinitivi.add(latLonToPos(pointObject.get("lat").getAsDouble(), pointObject.get("lon").getAsDouble(),
                            minLat + (maxLat - minLat) / 2, minLon + (maxLon - minLon) / 2));
                }
            }

            if (j.has("tags")) {
                JsonObject jtags = j.get("tags").getAsJsonObject();

                // Controlla se è un palazzo
                if (jtags.has("building")) {
                    JsonElement jbuilding = jtags.get("building");

                    IBlockState blockType = Blocks.CONCRETE.getDefaultState();
                    IBlockState windowType = Blocks.CONCRETE.getDefaultState().withProperty(BlockConcretePowder.COLOR, EnumDyeColor.SILVER);

                    int height = 7;
                    int floors = 2;

                    // Aggiungo la costruzione
                    inst.add(new BuildingStruct(puntiDefinitivi, blockType, windowType, height, floors));
                }
            }
        }
    }

    private BlockPos latLonToPos(double lat, double lon, double lat0, double lon0) {
        switch (proj) {
            case NAIVE_00:
                return GeoPos.from00Naive(lat, lon, (int) Minecraft.getMinecraft().player.posY);
            case NAIVE_PLAYER:
                return GeoPos.fromPlayerNaive(lat, lon, lat0, lon0);
            case BTE_00:
                return GeoPos.from00BTE(lat, lon, (int) Minecraft.getMinecraft().player.posY);
            case BTE_PLAYER:
                return GeoPos.fromPlayerBTE(lat, lon, lat0, lon0);
            default:
                System.err.println("Unknown Projection Found!");
                return BlockPos.ORIGIN;
        }
    }
}
