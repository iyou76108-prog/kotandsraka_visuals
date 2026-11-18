package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class VisualConfigScreen extends Screen {
    private final Screen parent;
    private int tab = 0;

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotAndSraka Visuals"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        VisualConfig c = VisualConfig.get();
        int cx = width / 2;
        String[] tabs = {"COMBAT","RENDER","HUD","MOVEMENT","COSMETIC"};
        for (int i=0;i<tabs.length;i++) {
            final int t=i;
            addRenderableWidget(Button.builder(Component.literal(tabs[i]), b->{tab=t;rebuild();})
                .bounds(cx-245+i*98,40,92,20).build());
        }

        int left=cx-105,right=cx+5,y=72;
        if(tab==0){
            add(left,y,"Hit Particles",c.hitParticles,v->{c.hitParticles=!c.hitParticles;save();});
            add(right,y,"Critical Particles",c.criticalParticles,v->{c.criticalParticles=!c.criticalParticles;save();});
            y+=28;
            add(left,y,"Hit Flash",c.hitFlash,v->{c.hitFlash=!c.hitFlash;save();});
            add(right,y,"Hit Text",c.hitText,v->{c.hitText=!c.hitText;save();});
            y+=28;
            add(left,y,"Damage Numbers",c.damageNumbers,v->{c.damageNumbers=!c.damageNumbers;save();});
            add(right,y,"Kill Effect",c.killEffect,v->{c.killEffect=!c.killEffect;save();});
            y+=28;
            add(left,y,"Target ESP",c.targetEsp,v->{c.targetEsp=!c.targetEsp;save();});
            add(right,y,"Hit Range",c.hitRange,v->{c.hitRange=!c.hitRange;save();});
            y+=28;
            add(left,y,"Custom Swing",c.customSwing,v->{c.customSwing=!c.customSwing;save();});
            add(right,y,"Trajectory",c.trajectory,v->{c.trajectory=!c.trajectory;save();});
            y+=36;
            addButton(left,y,"Particles: "+c.particleCount,b->{c.particleCount=c.particleCount>=24?2:c.particleCount+2;save();rebuild();});
            addButton(right,y,"Size: "+String.format("%.1f",c.particleScale),b->{c.particleScale=c.particleScale>=2?0.5f:c.particleScale+0.25f;save();rebuild();});
        } else if(tab==1){
            add(left,y,"Custom Crosshair",c.customCrosshair,v->{c.customCrosshair=!c.customCrosshair;save();});
            add(right,y,"View Model",c.viewModel,v->{c.viewModel=!c.viewModel;save();});
            y+=28;
            add(left,y,"Pearl Trail",c.pearlTrail,v->{c.pearlTrail=!c.pearlTrail;save();});
            add(right,y,"Custom Fire",c.customFire,v->{c.customFire=!c.customFire;save();});
            y+=28;
            add(left,y,"Motion Blur",c.motionBlur,v->{c.motionBlur=!c.motionBlur;save();});
            add(right,y,"World Particles",c.worldParticles,v->{c.worldParticles=!c.worldParticles;save();});
            y+=36;
            addButton(left,y,"Crosshair Size: "+c.crosshairSize,b->{c.crosshairSize=c.crosshairSize>=12?2:c.crosshairSize+2;save();rebuild();});
            addButton(right,y,"Gap: "+c.crosshairGap,b->{c.crosshairGap=c.crosshairGap>=8?0:c.crosshairGap+1;save();rebuild();});
            y+=28;
            addButton(left,y,"Thickness: "+c.crosshairThickness,b->{c.crosshairThickness=c.crosshairThickness>=4?1:c.crosshairThickness+1;save();rebuild();});
        } else if(tab==2){
            add(left,y,"Target HUD",c.targetHud,v->{c.targetHud=!c.targetHud;save();});
            add(right,y,"Keystrokes",c.keystrokes,v->{c.keystrokes=!c.keystrokes;save();});
            y+=28;
            add(left,y,"My Armor",c.armorHud,v->{c.armorHud=!c.armorHud;save();});
            add(right,y,"FPS",c.fpsHud,v->{c.fpsHud=!c.fpsHud;save();});
            y+=28;
            add(left,y,"CPS",c.cpsHud,v->{c.cpsHud=!c.cpsHud;save();});
            add(right,y,"Ping",c.pingHud,v->{c.pingHud=!c.pingHud;save();});
            y+=28;
            add(left,y,"Coordinates",c.coordsHud,v->{c.coordsHud=!c.coordsHud;save();});
            add(right,y,"Speed",c.speedHud,v->{c.speedHud=!c.speedHud;save();});
            y+=28;
            add(left,y,"Combo",c.comboHud,v->{c.comboHud=!c.comboHud;save();});
            add(right,y,"Totem",c.totemHud,v->{c.totemHud=!c.totemHud;save();});
            y+=28;
            add(left,y,"Potions",c.potionHud,v->{c.potionHud=!c.potionHud;save();});
            add(right,y,"Clock",c.clockHud,v->{c.clockHud=!c.clockHud;save();});
            y+=28;
            add(left,y,"Compass",c.compassHud,v->{c.compassHud=!c.compassHud;save();});
            addButton(right,y,"HUD X: "+c.hudX,b->{c.hudX=c.hudX>=220?0:c.hudX+10;save();rebuild();});
            y+=28;
            addButton(left,y,"HUD Y: "+c.hudY,b->{c.hudY=c.hudY>=180?70:c.hudY+10;save();rebuild();});
            addButton(right,y,"Armor X: "+c.armorHudX,b->{c.armorHudX=c.armorHudX>=200?0:c.armorHudX+10;save();rebuild();});
            y+=28;
            addButton(left,y,"Armor Y: "+c.armorHudY,b->{c.armorHudY=c.armorHudY>=150?0:c.armorHudY+10;save();rebuild();});
        } else if(tab==3){
            add(left,y,"Player Trail",c.playerTrail,v->{c.playerTrail=!c.playerTrail;save();});
            add(right,y,"Auto Sprint",c.autoSprint,v->{c.autoSprint=!c.autoSprint;save();});
            y+=28;
            add(left,y,"Jump Rings",c.jumpRings,v->{c.jumpRings=!c.jumpRings;save();});
            add(right,y,"Trail Intensity",c.trailIntensity>0,v->{c.trailIntensity=c.trailIntensity>=5?1:c.trailIntensity+1;save();rebuild();});
            y+=28;
            add(left,y,"Body Glow",c.bodyGlow,v->{c.bodyGlow=!c.bodyGlow;save();});
            add(right,y,"China Hat",c.chinaHat,v->{c.chinaHat=!c.chinaHat;save();});
        } else {
            add(left,y,"Glass Menu",c.glassMenu,v->{c.glassMenu=!c.glassMenu;save();});
            add(right,y,"Rounded Corners",c.roundedMenu,v->{c.roundedMenu=!c.roundedMenu;save();});
            y+=28;
            add(left,y,"Menu Animation",c.animatedMenu,v->{c.animatedMenu=!c.animatedMenu;save();});
            add(right,y,"Chroma Theme",c.chromaTheme,v->{c.chromaTheme=!c.chromaTheme;save();});
            y+=36;
            addButton(left,y,"Menu Scale: "+String.format("%.1f",c.menuScale),b->{c.menuScale=c.menuScale>=1.5f?0.75f:c.menuScale+0.25f;save();rebuild();});
            addButton(right,y,"Opacity: "+String.format("%.2f",c.hudOpacity),b->{c.hudOpacity=c.hudOpacity>=1?0.4f:c.hudOpacity+0.1f;save();rebuild();});
            y+=34;
            addButton(left,y,"RESET ALL",b->{VisualConfig.reset();rebuild();});
        }
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(cx-50,height-32,100,20).build());
    }

    private void save(){VisualConfig.save();rebuild();}
    private void add(int x,int y,String name,boolean enabled,Consumer<Button> action){
        addRenderableWidget(Button.builder(Component.literal(name+": "+(enabled?"ON":"OFF")),action::accept).bounds(x,y,100,20).build());
    }
    private void addButton(int x,int y,String name,Consumer<Button> action){
        addRenderableWidget(Button.builder(Component.literal(name),action).bounds(x,y,100,20).build());
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        this.renderBackground(g,mouseX,mouseY,delta);
        g.fill(0,0,width,height,VisualConfig.get().glassMenu?0x30000000:0x18000000);
        g.drawCenteredString(font,title,width/2,20,0xFFFFFFFF);
        g.drawCenteredString(font,"PulseVisual-inspired Combat / Render / HUD / Movement / Cosmetic",width/2,30,0xFFAAAAAA);
        super.render(g,mouseX,mouseY,delta);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
    @Override public boolean shouldCloseOnEsc(){return true;}
}
