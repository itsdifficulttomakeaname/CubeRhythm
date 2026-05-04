package org.cubeRhythm.manager;

import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.util.Util;
import org.cubeRhythm.coordinate.Face;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.cubeRhythm.Main.instance;

public class OffsetConfig {
    private static OffsetConfig inst;

    // 音符方块位置（每面）
    public double noteXW, noteXA, noteXS, noteXD;
    public double noteYW, noteYA, noteYS, noteYD;
    // FLICK 位置（每面）
    public double flickXW, flickXA, flickXS, flickXD;
    public double flickY;
    // 光标位置（每面）
    public double cursorYW, cursorYA, cursorYS, cursorYD;
    public double cursorZW, cursorZA, cursorZS, cursorZD;
    // 音符缩放
    public double scaleStartDist, scaleEndDist, scaleMin, scaleMax;
    public double scaleZFactor, holdScaleZFactor;
    // 光标视觉
    public double cursorHideDist, cursorAlphaBase, cursorAlphaDistRef, cursorAlphaFactor;
    public double cursorScaleDivisor, cursorOpacityMult, cursorAlphaLow;
    public double cursorTransXFactor, cursorTransYFactor;
    // FLICK 视觉
    public double flickBlockSize, flickBlockDepth, flickCenterXY, flickCenterZ;
    public double flickArrowScale;
    public double flickArrowXW, flickArrowXA, flickArrowXS, flickArrowXD;
    public double flickArrowYW, flickArrowYA, flickArrowYS, flickArrowYD;
    public double flickArrowZW, flickArrowZA, flickArrowZS, flickArrowZD;
    // 连接线
    public double connectLineWidth, connectLineTransOffset;
    public double connectLineOffsetXW, connectLineOffsetYW, connectLineOffsetZW;
    public double connectLineOffsetXA, connectLineOffsetYA, connectLineOffsetZA;
    public double connectLineOffsetXS, connectLineOffsetYS, connectLineOffsetZS;
    public double connectLineOffsetXD, connectLineOffsetYD, connectLineOffsetZD;
    // 判定特效
    public double hitEffectScale, hitEffectTransXFactor, hitEffectTransYFactor;
    public double hitEffectAlphaDecay, hitEffectScaleDecay, hitEffectScaleMin;
    public double hitEffectGlowBase, hitEffectGlowAlphaThreshold, hitEffectGlowAlphaMin;
    public int hitEffectDuration;
    public double hitEffectDecayExact, hitEffectDecayJust;

    public static OffsetConfig get() { return inst; }

    public static void init() {
        inst = new OffsetConfig();
        inst.reload();
    }

    public void reload() {
        Util.savePluginResource("offsets.yml");
        MapTree c;
        try {
            c = MapTree.fromYaml(Files.readString(new File(instance.getDataFolder(), "offsets.yml").toPath()));
        } catch (IOException e) {
            instance.getLogger().warning("Failed to load offsets.yml: " + e.getMessage());
            return;
        }

        noteXW = c.getDouble("note_x_w", 0.5);
        noteXA = c.getDouble("note_x_a", 1.5);
        noteXS = c.getDouble("note_x_s", 0.5);
        noteXD = c.getDouble("note_x_d", -2.5);

        noteYW = c.getDouble("note_y_w", 1.1);
        noteYA = c.getDouble("note_y_a", 1.1);
        noteYS = c.getDouble("note_y_s", 1.1);
        noteYD = c.getDouble("note_y_d", 1.1);

        flickXW = c.getDouble("flick_x_w", 0.0);
        flickXA = c.getDouble("flick_x_a", 1.0);
        flickXS = c.getDouble("flick_x_s", 0.0);
        flickXD = c.getDouble("flick_x_d", -2.0);
        flickY  = c.getDouble("flick_y",   1.1);

        cursorYW = c.getDouble("cursor_y_w", 1.6);
        cursorYA = c.getDouble("cursor_y_a", 1.6);
        cursorYS = c.getDouble("cursor_y_s", 1.6);
        cursorYD = c.getDouble("cursor_y_d", 1.6);

        cursorZW = c.getDouble("cursor_z_w", 4.0);
        cursorZA = c.getDouble("cursor_z_a", 4.0);
        cursorZS = c.getDouble("cursor_z_s", 4.0);
        cursorZD = c.getDouble("cursor_z_d", 4.0);

        scaleStartDist   = c.getDouble("scale_start_distance", 50.0);
        scaleEndDist     = c.getDouble("scale_end_distance",    4.0);
        scaleMin         = c.getDouble("scale_min",             0.2);
        scaleMax         = c.getDouble("scale_max",             1.0);
        scaleZFactor     = c.getDouble("scale_z_factor",        0.5);
        holdScaleZFactor = c.getDouble("hold_scale_z_factor",   5.0);

        cursorHideDist     = c.getDouble("cursor_hide_distance",   25.0);
        cursorAlphaBase    = c.getDouble("cursor_alpha_base",      100.0);
        cursorAlphaDistRef = c.getDouble("cursor_alpha_dist_ref",  4.0);
        cursorAlphaFactor  = c.getDouble("cursor_alpha_factor",    4.0);
        cursorScaleDivisor = c.getDouble("cursor_scale_divisor",   25.0);
        cursorOpacityMult  = c.getDouble("cursor_opacity_multiplier", 2.0);
        cursorAlphaLow     = c.getDouble("cursor_alpha_low",       10.0);
        cursorTransXFactor = c.getDouble("cursor_trans_x_factor",  0.015);
        cursorTransYFactor = c.getDouble("cursor_trans_y_factor",  0.15);

        flickBlockSize        = c.getDouble("flick_block_size",   5.0);
        flickBlockDepth       = c.getDouble("flick_block_depth",  1.0);
        flickCenterXY         = c.getDouble("flick_center_xy",   -2.5);
        flickCenterZ          = c.getDouble("flick_center_z",    -0.5);
        flickArrowScale       = c.getDouble("flick_arrow_scale", 15.0);

        flickArrowXW = c.getDouble("flick_arrow_x_w", 0.0);
        flickArrowXA = c.getDouble("flick_arrow_x_a", 0.0);
        flickArrowXS = c.getDouble("flick_arrow_x_s", 0.0);
        flickArrowXD = c.getDouble("flick_arrow_x_d", 0.0);
        flickArrowYW = c.getDouble("flick_arrow_y_w", -2.0);
        flickArrowYA = c.getDouble("flick_arrow_y_a", -2.0);
        flickArrowYS = c.getDouble("flick_arrow_y_s", -2.0);
        flickArrowYD = c.getDouble("flick_arrow_y_d", -2.0);
        flickArrowZW = c.getDouble("flick_arrow_z_w", 0.0);
        flickArrowZA = c.getDouble("flick_arrow_z_a", 0.0);
        flickArrowZS = c.getDouble("flick_arrow_z_s", 0.0);
        flickArrowZD = c.getDouble("flick_arrow_z_d", 0.0);

        connectLineWidth       = c.getDouble("connect_line_width",        0.1);
        connectLineTransOffset = c.getDouble("connect_line_trans_offset", 0.05);

        connectLineOffsetXW = c.getDouble("connect_line_offset_x_w", 0.0);
        connectLineOffsetYW = c.getDouble("connect_line_offset_y_w", 0.0);
        connectLineOffsetZW = c.getDouble("connect_line_offset_z_w", 0.0);
        connectLineOffsetXA = c.getDouble("connect_line_offset_x_a", 0.0);
        connectLineOffsetYA = c.getDouble("connect_line_offset_y_a", 0.0);
        connectLineOffsetZA = c.getDouble("connect_line_offset_z_a", 0.0);
        connectLineOffsetXS = c.getDouble("connect_line_offset_x_s", 0.0);
        connectLineOffsetYS = c.getDouble("connect_line_offset_y_s", 0.0);
        connectLineOffsetZS = c.getDouble("connect_line_offset_z_s", 0.0);
        connectLineOffsetXD = c.getDouble("connect_line_offset_x_d", 0.0);
        connectLineOffsetYD = c.getDouble("connect_line_offset_y_d", 0.0);
        connectLineOffsetZD = c.getDouble("connect_line_offset_z_d", 0.0);

        hitEffectScale          = c.getDouble("hit_effect_scale",               3.0);
        hitEffectTransXFactor   = c.getDouble("hit_effect_trans_x_factor",      0.015);
        hitEffectTransYFactor   = c.getDouble("hit_effect_trans_y_factor",      0.15);
        hitEffectAlphaDecay     = c.getDouble("hit_effect_alpha_decay",         24.0);
        hitEffectScaleDecay     = c.getDouble("hit_effect_scale_decay",         0.9);
        hitEffectScaleMin       = c.getDouble("hit_effect_scale_min",           1.0);
        hitEffectGlowBase       = c.getDouble("hit_effect_glow_base",           8.0);
        hitEffectGlowAlphaThreshold = c.getDouble("hit_effect_glow_alpha_threshold", 40.0);
        hitEffectGlowAlphaMin   = c.getDouble("hit_effect_glow_alpha_min",      14.0);
        hitEffectDuration       = (int) c.getDouble("hit_effect_duration",      10.0);
        hitEffectDecayExact     = c.getDouble("hit_effect_decay_exact",         0.7);
        hitEffectDecayJust      = c.getDouble("hit_effect_decay_just",          0.5);
    }

    public double noteX(Face face) {
        return switch (face) { case W -> noteXW; case A -> noteXA; case S -> noteXS; case D -> noteXD; };
    }
    public double noteY(Face face) {
        return switch (face) { case W -> noteYW; case A -> noteYA; case S -> noteYS; case D -> noteYD; };
    }
    public double flickX(Face face) {
        return switch (face) { case W -> flickXW; case A -> flickXA; case S -> flickXS; case D -> flickXD; };
    }
    public double cursorY(Face face) {
        return switch (face) { case W -> cursorYW; case A -> cursorYA; case S -> cursorYS; case D -> cursorYD; };
    }
    public double cursorZ(Face face) {
        return switch (face) { case W -> cursorZW; case A -> cursorZA; case S -> cursorZS; case D -> cursorZD; };
    }
    public double flickArrowX(Face face) {
        return switch (face) { case W -> flickArrowXW; case A -> flickArrowXA; case S -> flickArrowXS; case D -> flickArrowXD; };
    }
    public double flickArrowY(Face face) {
        return switch (face) { case W -> flickArrowYW; case A -> flickArrowYA; case S -> flickArrowYS; case D -> flickArrowYD; };
    }
    public double flickArrowZ(Face face) {
        return switch (face) { case W -> flickArrowZW; case A -> flickArrowZA; case S -> flickArrowZS; case D -> flickArrowZD; };
    }
    public double connectLineOffsetX(Face face) {
        return switch (face) { case W -> connectLineOffsetXW; case A -> connectLineOffsetXA; case S -> connectLineOffsetXS; case D -> connectLineOffsetXD; };
    }
    public double connectLineOffsetY(Face face) {
        return switch (face) { case W -> connectLineOffsetYW; case A -> connectLineOffsetYA; case S -> connectLineOffsetYS; case D -> connectLineOffsetYD; };
    }
    public double connectLineOffsetZ(Face face) {
        return switch (face) { case W -> connectLineOffsetZW; case A -> connectLineOffsetZA; case S -> connectLineOffsetZS; case D -> connectLineOffsetZD; };
    }
}
