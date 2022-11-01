package com.mandatory_overtime.controller;


import com.mandatory_overtime.model.Building;
import com.mandatory_overtime.model.Building.CantGetItemException;
import com.mandatory_overtime.model.exception.MissingRequirementException;
import com.mandatory_overtime.view.GuiView;
import com.mandatory_overtime.view.MapDialog;
import com.mandatory_overtime.view.SettingsMenu;
import com.mandatory_overtime.view.UserView;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.UIManager;



public class GUIController {

    private GuiView view;

    private final JButton startBtn;

    private final JButton loadBtn;
    private Building building = new Building();

    private String message = "";

    private String removedItem;

    private UserView stringMessages = new UserView();
    private MapDialog map;

    private boolean godMode = false;

    public GUIController() throws IOException {
        UIManager.put("OptionPane.minimumSize", new Dimension(200,150));
        view = new GuiView();
        view.presentMainMenu();
        loadBtn = view.getLoadGameButton();
        startBtn = view.getNewGameButton();
        loadActionEvents();
    }

    public void startNewGame() throws IOException {
        // Create New Building
        building.createGameStructureFromNew();
        String playerName =  JOptionPane.showInputDialog("What is you name? ");

        if(playerName == null){
            return;
        } else if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(null,"Please enter a valid name to start game");
            return;
        }
        building.setName(playerName);
        setUpGamePlayHandlers();
        view.getMenuBar().getSettingsDialog().getToggleCheatCheckbox().setEnabled(true);
    }

    public void loadGame() throws IOException {
        try {
            building.createGameStructureFromSave();
            message = "Game Loaded";
        } catch (IOException | URISyntaxException e) {
            JOptionPane.showMessageDialog(null,"No saved game found. Starting a new game");
            startNewGame();

        }finally {
            setUpGamePlayHandlers();
        }
    }
    public void updateGameView() {
        String currentLocation = building.getPlayer().getCurrentLocation();
        String[] directions = building.getBuilding().get(currentLocation).getDirections();
        List<String> inventory = building.getPlayer().getInventory();
        view.updateGameScreen(currentLocation, inventory, message, directions, removedItem);
    }

    public void loadActionEvents() {
        startBtn.addActionListener(e -> {
            try {
                startNewGame();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        loadBtn.addActionListener(e -> {
            try {
                loadGame();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    public void setUpGamePlayHandlers() throws IOException {
        map = new MapDialog(building.getPlayer().getCurrentLocation());
        // Set up Game Screen
        view.setUpGamePlay(building.getBuilding(), building.getGameItems());
        String currentLocationStart = building.getPlayer().getCurrentLocation();
        message = building.getBuilding().get(currentLocationStart).getDescription();

        // Set up Move Consumers for Game
        view.setMoveConsumer(roomName -> {
            try {
                building.moveRooms2(roomName);
                String currentLocation = building.getPlayer().getCurrentLocation();
                String description = building.getBuilding().get(currentLocation).getDescription();
                String item = building.getBuilding().get(currentLocation).getItem();
                message = stringMessages.gameStatus(currentLocation,description,item );
                updateGameView();
            } catch (MissingRequirementException | InterruptedException e) {
                message = e.getMessage();
                updateGameView();
            }
        });

        //Set up Item pickup consumer
        view.getGamePlayScreen().getLocationGUIPanel().setItemPickupListener(
            itemName -> {
                try {
                    building.getItem(itemName);
                    removedItem = itemName;
                    message = "You picked up " + itemName;
                    updateGameView();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (MissingRequirementException | CantGetItemException e){
                    message = e.getMessage();
                }
            });

        view.getGamePlayScreen().getLocationGUIPanel().setNpcListener(npcName -> {
                message = building.interactWithNpc(npcName);
                updateGameView();
            }
        );

        view.setSaveListener(() -> {
            try {
                building.gameSave();
                message = "Game Saved";
                updateGameView();
            } catch (IOException e) {
                message = "An error occurred trying to save the game";
                updateGameView();
            }
        });

        view.getMenuBar().setGodModeAction(() -> {
            building.getAllItems("mode");
            updateGameView();
        });

        view.setQuitListener(() -> {
            try {
                building.gameSave();
                JOptionPane.showMessageDialog(null,"Game Saved!...Closing Window");
                System.exit(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        // Set Message TO Intro Story
       // message = "Starting in the Office";

        // Update View
        updateGameView();

        // Present View
        view.presentGameScreen();
    }
}
