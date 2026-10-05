package org.geysermc.hydraulic.entity;

import com.google.gson.*;
import java.util.*;

/** Export the 26.3 boat model's parts and UV layout without private Bedrock aliases. */
public final class JavaBoatGeometry {
    public static JsonObject create(String identifier, boolean chest) {
        var bones = new JsonArray();
        part(bones,"bottom",new float[]{0,3,1},new float[]{90,0,0},cube(-14,-9,-3,28,16,3,0,0));
        part(bones,"back",new float[]{-15,4,4},new float[]{0,270,0},cube(-13,-7,-1,18,6,2,0,19));
        part(bones,"front",new float[]{15,4,0},new float[]{0,90,0},cube(-8,-7,-1,16,6,2,0,27));
        part(bones,"right",new float[]{0,4,-9},new float[]{0,180,0},cube(-14,-7,-1,28,6,2,0,35));
        part(bones,"left",new float[]{0,4,9},new float[]{0,0,0},cube(-14,-7,-1,28,6,2,0,43));
        part(bones,"left_paddle",new float[]{3,-5,9},new float[]{0,0,11.25f},cube(-1,0,-5,2,2,18,62,0),cube(-1.001f,-3,8,1,6,7,62,0));
        part(bones,"right_paddle",new float[]{3,-5,-9},new float[]{0,180,11.25f},cube(-1,0,-5,2,2,18,62,20),cube(.001f,-3,8,1,6,7,62,20));
        if(chest) {
            part(bones,"chest_bottom",new float[]{-2,-5,-6},new float[]{0,-90,0},cube(0,0,0,12,8,12,0,76));
            part(bones,"chest_lid",new float[]{-2,-9,-6},new float[]{0,-90,0},cube(0,0,0,12,4,12,0,59));
            part(bones,"chest_lock",new float[]{-1,-6,-1},new float[]{0,-90,0},cube(0,0,0,2,4,1,0,59));
        }
        var description = new Gson().toJsonTree(Map.of("identifier",identifier,"texture_width",128,"texture_height",chest?128:64,
                "visible_bounds_width",4,"visible_bounds_height",3,"visible_bounds_offset",List.of(0,0,0))).getAsJsonObject();
        var geometry = new JsonObject(); geometry.add("description",description); geometry.add("bones",bones);
        var geometries = new JsonArray(); geometries.add(geometry);
        var result = new JsonObject(); result.addProperty("format_version","1.12.0"); result.add("minecraft:geometry",geometries); return result;
    }
    private static JsonObject cube(float x,float y,float z,float sx,float sy,float sz,int u,int v) {
        return new Gson().toJsonTree(Map.of("origin",List.of(x,y,z),"size",List.of(sx,sy,sz),"uv",List.of(u,v))).getAsJsonObject();
    }
    private static void part(JsonArray bones,String name,float[] pivot,float[] rotation,JsonObject... cubes) {
        var bone = new JsonObject(); bone.addProperty("name",name);
        var gson = new Gson(); bone.add("pivot",gson.toJsonTree(new float[]{pivot[0],-pivot[1],pivot[2]}));
        bone.add("rotation",gson.toJsonTree(new float[]{-rotation[0],rotation[1],-rotation[2]}));
        var output = new JsonArray();
        for(var source:cubes) {
            var c=source.deepCopy();var p=c.getAsJsonArray("origin");var size=c.getAsJsonArray("size");
            c.add("origin",gson.toJsonTree(new float[]{pivot[0]+p.get(0).getAsFloat(),-pivot[1]-p.get(1).getAsFloat()-size.get(1).getAsFloat(),pivot[2]+p.get(2).getAsFloat()}));
            output.add(c);
        }
        bone.add("cubes",output);bones.add(bone);
    }
}
