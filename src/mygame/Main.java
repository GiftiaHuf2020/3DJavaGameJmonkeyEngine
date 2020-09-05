package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture; 
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.BaseStyles;

/**
 *
 * @author Huf
 */
public class Main extends SimpleApplication implements ActionListener{

    //Start Game controls
    private int StartGame = 0;
    private int GameUpdateStart = 0;
    
    private Spatial sceneModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private CharacterControl player;    
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    //Monster
    private CharacterControl monster1;
    private Node node1;

    //model and boss hp 
    private int robothp = 1000;
    private Spatial Cirno;
    //UI for boss hp 
    private BitmapText BossHp;
    //UI phsic
    
    //Gametime
    private BitmapText GameTime;

    //Audio
    private AudioNode audio_gun;
    private AudioNode audio_nature;
    
    //Timer
    private double secondcountdown = 60.00;
    private int minutes = 30;
    private int SecondCovert;
    
    //Ammor for weapon
    BitmapText Ammor;
    private int weaponammor = 30;
    private double reloadtimecount = 0;
    private int reloadtime = 3;
    
    //Game menu
    Picture bgMenu;
    
    //Game control (Win or lose)
    private int win = 0;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Touhou Cirno Fighting War");// 标题
        settings.setResolution(1024, 600);// 分辨率

        Main app = new Main();
        app.setSettings(settings);// 应用参数
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        app.setShowSettings(false);
        app.start();
    }

    private Node shootables;
    private Geometry mark;

    @Override
    public void simpleInitApp() {
        if(StartGame==0){
            flyCam.setEnabled(false);
            
            GuiGlobals.initialize(this);
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
            final Container myWindows = new Container();
            guiNode.attachChild(myWindows);
            
            //Game Menu Background
            bgMenu = new Picture("HUD Picture(TOP RIGHT)");
            bgMenu.setImage(assetManager, "Pictures/Background.jpg", true);
            bgMenu.setWidth(1024);
            bgMenu.setHeight(600);
            bgMenu.setPosition(0, 0);
            guiNode.attachChild(bgMenu);
            
            myWindows.setLocalTranslation(50,300,0);
            myWindows.addChild(new Label("Welcome to Touhou Cirno Fighting War"));
            
            Button clickme = myWindows.addChild(new Button("Start Game"));
            clickme.addClickCommands(new Command<Button>(){
                @Override
                public void execute(Button source){
                    System.out.println("Hello Worlds You Click the button");
                    //StartGame = 1;
                    guiNode.detachChild(bgMenu);
                    guiNode.detachChild(myWindows);
                    flyCam.setEnabled(true);
                    inputManager.setCursorVisible(false);
                    initGame();
                }
            });
            
            Button Exitgame = myWindows.addChild(new Button("Exit Game"));
            Exitgame.addClickCommands(new Command<Button>(){
                @Override
                public void execute(Button source){
                    System.out.println("Exit game");
                    System.exit(0);
                }
            });
            
        }
    }
    
    //Initialise Game function
    private void initGame(){
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning 
        // is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpKeys();
        setUpLight();

        //KEY AND CROSSHAIR FUNCTION
        initCrossHairs(); // a "+" in the middle of the screen to help aiming
        initKeys();       // load custom key mappings
        initMark();       // a red sphere to mark the hit

        //Create a character
        Cirno = assetManager.loadModel("Models/Cirno.j3o");
        Material mat = assetManager.loadMaterial("Materials/Cirno.j3m");
        Cirno.setMaterial(mat);
        Cirno.scale(0.02f, 0.02f, 0.02f);
        Cirno.rotate(0.0f, -3.0f, 0.0f);
        Cirno.setLocalTranslation(30, 1.0f, 30);
        //rootNode.attachChild(Cirno);

        /** create four colored boxes and a floor to shoot at: */
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);
        //shootables.attachChild(makeCube("a Dragon", -2f, 5f, 1f));
        //shootables.attachChild(makeCube("a tin can", 1f, -7f, 0f));
        //shootables.attachChild(makeCube("the Sheriff", 0f, 6f, -2f));
        //shootables.attachChild(makeCube("the Deputy", 1f, 5f, -4f));
        shootables.attachChild(Cirno);
        //shootables.attachChild(makeFloor());
        //shootables.attachChild(makeCharacter());

        // We load the scene from the zip file and adjust its size.
        /*assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");*/
        Spatial sceneModel = assetManager.loadModel("Scenes/newScene2.j3o");
        sceneModel.setLocalScale(2f);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        /**
         * We set up collision detection for the player by creating
         * a capsule collision shape and a CharacterControl.
         * The CharacterControl offers extra settings for
         * size, stepheight, jumping, falling, and gravity.
         * We also put the player in its starting position.
         */
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);

        // You can change the gravity of individual physics objects before or after
        //they are added to the PhysicsSpace, but it must be set before MOVING the
        //physics location.
        player.setGravity(new Vector3f(0,-30f,0));
        player.setPhysicsLocation(new Vector3f(0, 10, 0));

        //Init Sound
        initAudio();
        
        /*
            UI interface
        */
        //BOTTOM LEFT (HP)
        Picture picbg = new Picture("HUD Picture(BOTTOM LEFT)");
        picbg.setImage(assetManager, "Pictures/profile.png", true);
        picbg.setWidth(170);
        picbg.setHeight(95);
        picbg.setPosition(25, 6);
        guiNode.attachChild(picbg);
        
        Picture pic = new Picture("HUD Picture(BOTTOM LEFT)");
        //pic.setImage(assetManager, "Textures/ColoredTex/Monkey.png", true);
        pic.setImage(assetManager, "Pictures/dz1.jpg", true);
        pic.setWidth(35);
        pic.setHeight(45);
        pic.setPosition(45, 33);
        guiNode.attachChild(pic);

        BitmapText HP = new BitmapText(guiFont, false);
        HP.setSize(guiFont.getCharSet().getRenderedSize());
        HP.setText("1000 / 1000\n" + "| | | | | | | | | | |\n" + "````````````````````````````");//30 " ` "
        HP.setColor(ColorRGBA.Black);
        HP.setLocalTranslation(85, 80, 0);
        guiNode.attachChild(HP);

        //BOTTOM RIGHT (WEAPON)
        Picture weaponbg = new Picture("HUD Picture(BOTTOM RIGHT)");
        weaponbg.setImage(assetManager, "Pictures/weapon.png", true);
        weaponbg.setWidth(170);
        weaponbg.setHeight(95);
        weaponbg.setPosition(settings.getWidth()-190, 5);
        guiNode.attachChild(weaponbg);
        
        Picture pic1 = new Picture("HUD Picture(BOTTOM RIGHT)");
        pic1.setImage(assetManager, "Pictures/ak471.png", true);
        pic1.setWidth(130);
        pic1.setHeight(35);
        pic1.setPosition(settings.getWidth()-170, 45);
        guiNode.attachChild(pic1);

        Ammor = new BitmapText(guiFont, false);
        Ammor.setSize(guiFont.getCharSet().getRenderedSize());
        Ammor.setText("30 / --");
        Ammor.setColor(ColorRGBA.Black);
        Ammor.setLocalTranslation(settings.getWidth()-110, 45, 0);
        guiNode.attachChild(Ammor);

        //TOP LEFT (WAVE)
        Picture wavebg = new Picture("HUD Picture(TOP LEFT)");
        wavebg.setImage(assetManager, "Pictures/wave.png", true);
        wavebg.setWidth(195);
        wavebg.setHeight(55);
        wavebg.setPosition(30, settings.getHeight()-61);
        guiNode.attachChild(wavebg);

        BitmapText Wave = new BitmapText(guiFont, false);
        Wave.setSize(guiFont.getCharSet().getRenderedSize());
        Wave.setText("WAVE 15  00:00\n" + "Final Wave");
        Wave.setColor(ColorRGBA.Black);
        Wave.setLocalTranslation(100, settings.getHeight()-15, 0);
        guiNode.attachChild(Wave);

        //TOP RIGHT (BOSS HP)       
        Picture bossbg = new Picture("HUD Picture(TOP RIGHT)");
        bossbg.setImage(assetManager, "Pictures/bosshp.png", true);
        bossbg.setWidth(190);
        bossbg.setHeight(55);
        bossbg.setPosition(settings.getWidth()-220, settings.getHeight()-61);
        guiNode.attachChild(bossbg);

        BossHp = new BitmapText(guiFont, false);
        BossHp.setSize(guiFont.getCharSet().getRenderedSize());
        BossHp.setText(" BOSS HP :\n" + robothp + "  / 1000");
        BossHp.setColor(ColorRGBA.Black);
        BossHp.setLocalTranslation(settings.getWidth()-220, settings.getHeight()-15, 0);
        guiNode.attachChild(BossHp);

        //CENTER TOP (TIME)
        //CENTER TOP (TIME background)
        Picture timebg = new Picture("HUD Picture(Center TOP)");
        timebg.setImage(assetManager, "Pictures/timebg1.png", true);
        timebg.setWidth(190);
        timebg.setHeight(40);
        timebg.setPosition((settings.getWidth()/2)-95, settings.getHeight()-55);
        guiNode.attachChild(timebg);

        GameTime = new BitmapText(guiFont, false);
        GameTime.setSize(guiFont.getCharSet().getRenderedSize());
        GameTime.setText("Times : 30 : 00");
        GameTime.setColor(ColorRGBA.Black);
        GameTime.setLocalTranslation((settings.getWidth()/2)-53, settings.getHeight()-25, 0);
        guiNode.attachChild(GameTime);
               
        //GameUpdateStart
        GameUpdateStart = 1;
    }

    //Audio Shoot sound
    private void initAudio() {
        /* gun shot sound is to be triggered by a mouse click. */
        audio_gun = new AudioNode(assetManager, "Sound/Effects/Gun.wav", AudioData.DataType.Buffer);
        audio_gun.setPositional(false);
        audio_gun.setLooping(false);
        audio_gun.setVolume(2);
        rootNode.attachChild(audio_gun);

        /* nature sound - keeps playing in a loop. */
        audio_nature = new AudioNode(assetManager, "Music/cirno.wav", AudioData.DataType.Stream);
        audio_nature.setLooping(true);  // activate continuous playing
        audio_nature.setPositional(true);
        audio_nature.setVolume(3);
        rootNode.attachChild(audio_nature);
        audio_nature.play(); // play continuously!
    }

    //Set light
    private void setUpLight() {
        // We add light so we see the scene
//        AmbientLight al = new AmbientLight();
//        al.setColor(ColorRGBA.White.mult(1.3f));
//        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping: */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }

    /** These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed.
     * @param binding */
    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left":
                left = isPressed;
                break;
            case "Right":
                right= isPressed;
                break;
            case "Up":
                up = isPressed;
                break;
            case "Down":
                down = isPressed;
                break;
            case "Jump":
                if (isPressed) { player.jump(new Vector3f(0,20f,0));}
                break;
            default:
                break;
        }
    }

    /**
     * This is the main event loop--walking happens here.
     * We check in which direction the player is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled player walk.
     * We also make sure here that the camera moves with player.
     * @param tpf
     */
    @Override
    public void simpleUpdate(float tpf) {
        
        if(GameUpdateStart==1){//start game controls
        
        
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());

        //MUSIC audio follow player
        listener.setLocation(cam.getLocation());
        listener.setRotation(cam.getRotation());
        
        //Update boss hp
        BossHp.setText("Cirno HP :\n" + robothp + " / 1000");
        
        //Game time
        //Timer
        if(SecondCovert>60){ //If over 60 seconds
        secondcountdown = 1; //reset seconds value
        minutes-=1; //Decrease 1minutes if over 60 seconds
        }
        //If the game no over 30 minutes
        if(minutes>-1){
        secondcountdown += tpf; // Increase 1 every 1s
        SecondCovert = (int) secondcountdown; //Convert float to int
            if(SecondCovert<61){
                //If more than 50 seconds or equal 60 second
                if(SecondCovert>50 || SecondCovert==60){
                    //Game time
                    GameTime.setText("Times : " + minutes + " : 0" + (60-SecondCovert));
                    //System.out.println("Minutes: " + minutes + " : " + "Seconds : 0" + (60-SecondCovert));
                }
                else{
                    //Game time
                    GameTime.setText("Times : " + minutes + " : " + (60-SecondCovert));                
                    //System.out.println("Minutes: " + minutes + " : " + "Seconds : " + (60-SecondCovert));
                }
            }
        }
        else{
            System.out.println("You lose the game.");
        }
        
        //Weapon ammor
        Ammor.setText(weaponammor + " / --");
        if(weaponammor<1){
            reloadtimecount += tpf; //Increase 1 every 1s
            if(reloadtime == (int) reloadtimecount){
                weaponammor = 30;
                reloadtimecount = 0;
            } //Convert float to int         
        }
        
        }//Start game control
    }

    /** Declaring the "Shoot" action and mapping to its triggers. */
    private void initKeys() {
        inputManager.addMapping("Shoot",
                //new KeyTrigger(KeyInput.KEY_SPACE), // trigger 1: spacebar
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT)); // trigger 2: left-button click
        inputManager.addListener(actionListener, "Shoot");
    }
    /** Defining the "Shoot" action: Determine what was hit and how to respond. */
    private final ActionListener actionListener = new ActionListener() {

        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Shoot") && !keyPressed) {
                if(weaponammor==0){ //Can't shoot when no ammor
                    //Wait update loop to refill ammor
                }
                else{
                    audio_gun.playInstance(); // play each instance once!
                    // 1. Reset results list.
                    CollisionResults results = new CollisionResults();


                    // 2. Aim the ray from cam loc to cam direction.
                    Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                    // 3. Collect intersections between Ray and Shootables in results list.
                    // DO NOT check collision with the root node, or else ALL collisions will hit the
                    // skybox! Always make a separate node for objects you want to collide with.
                    shootables.collideWith(ray, results);
                    // 4. Print the results
                    System.out.println("----- Collisions? " + results.size() + "-----");
                    for (int i = 0; i < results.size(); i++) {
                        // For each hit, we know distance, impact point, name of geometry.
                        float dist = results.getCollision(i).getDistance();
                        Vector3f pt = results.getCollision(i).getContactPoint();
                        String hit = results.getCollision(i).getGeometry().getName();
                        System.out.println("* Collision #" + i);
                        System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");

                    }
                    // 5. Use the results (we mark the hit object)
                    if (results.size() > 0) {
                        // The closest collision point is what was truly hit:
                        CollisionResult closest = results.getClosestCollision();
                        // Let's interact - we mark the hit with a red dot.
                        mark.setLocalTranslation(closest.getContactPoint());
                        rootNode.attachChild(mark);

                        Geometry g = closest.getGeometry();
                        if(robothp > 0 ){
                            robothp -= 30;
                            System.out.println("-30 hp");
                            System.out.println(g.getName());
                            if (g.getName().equals("Cirno-geom-0")) {
                                System.out.println("Cirno!!!");  
                            }
                        }
                        else{
                            shootables.detachChild(Cirno);
                            winlose();//if boss hp equal 0 then display game win
                        }

                    } else {
                        // No hits? Then remove the red mark.
                        rootNode.detachChild(mark);
                    }
                    weaponammor--;
                
                }
            }
        }
    };

    /** A cube object for target practice
     * @param name
     * @param x
     * @param y
     * @param z
     * @return  */
    protected Geometry makeCube(String name, float x, float y, float z) {
        Box box = new Box(1, 1, 1);
        Geometry cube = new Geometry(name, box);
        cube.setLocalTranslation(x, y, z);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.randomColor());
        cube.setMaterial(mat1);
        return cube;
    }

    /** A red ball that marks the last spot that was "hit" by the "shot". */
    protected void initMark() {
        Sphere sphere = new Sphere(30, 30, 0.2f);
        mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }

    /** A centred plus sign to help the player aim. */
    protected void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
        settings.getWidth() / 2 - ch.getLineWidth()/2,
        settings.getHeight() / 2 + ch.getLineHeight()/2, 0);
        guiNode.attachChild(ch);
    }

    protected Spatial makeCharacter() {
        // load a character from jme3test-test-data
        Spatial golem = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        golem.scale(0.5f);
        golem.setLocalTranslation(-1.0f, 4.5f, -0.6f);

        //We must add a light to make the model visible
        //DirectionalLight sun = new DirectionalLight();
        //sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        //golem.addLight(sun);
        return golem;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    protected void winlose(){
        Picture Win = new Picture("HUD Picture(Center)");
        Win.setImage(assetManager, "Pictures/Victory.png", true);
        Win.setWidth(542);
        Win.setHeight(324);
        Win.setPosition(247, 168);
        guiNode.attachChild(Win);      
        //System.out.println("WIN");
        guiNode.detachChild(GameTime);
        guiNode.detachChild(BossHp);
    }
}
