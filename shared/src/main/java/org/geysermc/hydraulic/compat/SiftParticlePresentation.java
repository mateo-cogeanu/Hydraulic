package org.geysermc.hydraulic.compat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.*;

/** Small finite emitters based on the original Java particle lifetimes and motion. */
public final class SiftParticlePresentation {
    public static JsonObject create(String identifier, String texture, int width, int height) {
        String name=identifier.substring(identifier.indexOf(':')+1);
        boolean note=name.equals("sift_note"), fragment=name.equals("sift_parallax"), mist=name.equals("ichor_surface_mist"), wave=name.contains("sound_wave");
        Map<String,Object> components=new LinkedHashMap<>();
        components.put("minecraft:emitter_rate_instant",Map.of("num_particles",1));
        components.put("minecraft:emitter_lifetime_once",Map.of("active_time",0.01));
        components.put("minecraft:emitter_initialization",Map.of("creation_expression",
                "variable.life = variable.life ?? "+(note?"math.random(5,10)":wave?"0.5":mist?"math.random(2,3.5)":"1.2")+"; variable.dx = variable.dx ?? 0; variable.dy = variable.dy ?? 1; variable.dz = variable.dz ?? 0; variable.speed = variable.speed ?? "+(note?"0.12":wave||mist?"0":"0.15")+"; variable.r = variable.r ?? 1; variable.g = variable.g ?? 1; variable.b = variable.b ?? 1;"));
        components.put("minecraft:emitter_shape_point",Map.of("offset",List.of(0,0,0),"direction",List.of("variable.dx","variable.dy","variable.dz")));
        components.put("minecraft:particle_initial_speed","variable.speed");
        components.put("minecraft:particle_lifetime_expression",Map.of("max_lifetime","variable.life"));
        components.put("minecraft:particle_motion_dynamic",Map.of("linear_acceleration",List.of(0,fragment?-16:mist?0:0.1,0),"linear_drag_coefficient",fragment?0.4:note?0.1:0));
        if(fragment) components.put("minecraft:particle_initialization",Map.of("creation_expression","variable.u = math.random(0,0.4); variable.v = math.random(0,0.4); variable.fw = math.random(0.3,0.6); variable.fh = math.random(0.3,0.6);"));
        Map<String,Object> uv=new LinkedHashMap<>();uv.put("texture_width",width);uv.put("texture_height",height);
        uv.put("uv",fragment?List.of("variable.u * "+width,"variable.v * "+height):List.of(0,0));
        uv.put("uv_size",fragment?List.of("variable.fw * "+width,"variable.fh * "+height):List.of(width,height));
        components.put("minecraft:particle_appearance_billboard",Map.of("size",note?List.of(0.4,0.4):mist?List.of(0.35,0.35):wave?
                List.of("0.2 * (1 + variable.particle_age / variable.particle_lifetime)","0.2 * (1 + variable.particle_age / variable.particle_lifetime)"):List.of(0.12,0.12),
                "facing_camera_mode",mist?"direction_z":"lookat_xyz","uv",uv));
        if(mist) {
            @SuppressWarnings("unchecked") var billboard = new LinkedHashMap<>((Map<String,Object>)components.get("minecraft:particle_appearance_billboard"));
            billboard.put("direction",Map.of("mode","custom_direction","custom_direction",List.of(0,1,0)));
            components.put("minecraft:particle_appearance_billboard",billboard);
        }
        String alpha=mist?"math.sin(180 * variable.particle_age / variable.particle_lifetime) * 0.55":note?"math.min(1, variable.particle_age / 0.7) * math.min(1, (variable.particle_lifetime-variable.particle_age) / 1.2) * 0.85":"1 - variable.particle_age / variable.particle_lifetime";
        components.put("minecraft:particle_appearance_tinting",Map.of("color",List.of("variable.r","variable.g","variable.b",alpha)));
        return new Gson().toJsonTree(Map.of("format_version","1.10.0","particle_effect",Map.of("description",Map.of("identifier",identifier,
                "basic_render_parameters",Map.of("material","particles_blend","texture",texture)),"components",components))).getAsJsonObject();
    }
}
