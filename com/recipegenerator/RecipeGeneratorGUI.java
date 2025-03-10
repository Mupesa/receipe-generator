package com.recipegenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

public class RecipeGeneratorGUI extends JFrame {
    private JTextField ingredientField;
    private JTextArea recipeArea;
    private JButton searchButton;
    private JPanel recipePanel;
    private JProgressBar progressBar;
    private static final String API_KEY = " "; //Spoonacular API key
    private static final String API_URL = "https://api.spoonacular.com/recipes/findByIngredients";
    private static final String RECIPE_INFO_URL = "https://api.spoonacular.com/recipes/%d/information";
    //bb44be3321c6481a97ddabf6c95b7401

    public RecipeGeneratorGUI() {
        // Set up the JFrame
        setTitle("Recipe Generator");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create components
        ingredientField = new JTextField();
        searchButton = new JButton("Search Recipes");
        recipeArea = new JTextArea();
        recipeArea.setEditable(false);
        recipePanel = new JPanel();
        recipePanel.setLayout(new BoxLayout(recipePanel, BoxLayout.Y_AXIS));
        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        // Add components to the JFrame
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(new JLabel("Enter Ingredients:"), BorderLayout.WEST);
        topPanel.add(ingredientField, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(recipePanel), BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        // Add action listener to the button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ingredients = ingredientField.getText();
                if (!ingredients.isEmpty()) {
                    fetchRecipes(ingredients);
                } else {
                    JOptionPane.showMessageDialog(RecipeGeneratorGUI.this, "Please enter some ingredients.");
                }
            }
        });
    }

    private void fetchRecipes(String ingredients) {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true); // Show loading indicator
        recipePanel.removeAll(); // Clear previous results
        recipePanel.revalidate();
        recipePanel.repaint();

        new Thread(() -> {
            try {
                // Encode ingredients for the URL
                String encodedIngredients = URLEncoder.encode(ingredients, "UTF-8");

                // Construct the API URL using URI
                URI uri = new URI(API_URL + "?ingredients=" + encodedIngredients + "&apiKey=" + API_KEY);
                URL url = uri.toURL();

                // Open a connection to the API
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONArray recipes = new JSONArray(response.toString());
                if (recipes.length() == 0) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(RecipeGeneratorGUI.this, "No recipes found.");
                        progressBar.setVisible(false);
                    });
                    return;
                }

                // Display the recipes in the panel
                for (int i = 0; i < recipes.length(); i++) {
                    JSONObject recipe = recipes.getJSONObject(i);
                    int id = recipe.getInt("id");
                    String title = recipe.getString("title");
                    String imageUrl = recipe.getString("image");

                    // Create a panel for each recipe
                    JPanel recipeCard = new JPanel();
                    recipeCard.setLayout(new BorderLayout());
                    recipeCard.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

                    // Add recipe image
                    ImageIcon imageIcon = new ImageIcon(new URI(imageUrl).toURL());
                    JLabel imageLabel = new JLabel(new ImageIcon(imageIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    recipeCard.add(imageLabel, BorderLayout.WEST);

                    // Add recipe title
                    JLabel titleLabel = new JLabel(title);
                    recipeCard.add(titleLabel, BorderLayout.CENTER);

                    // Add "View Details" button
                    JButton detailsButton = new JButton("View Details");
                    detailsButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            fetchRecipeDetails(id);
                        }
                    });
                    recipeCard.add(detailsButton, BorderLayout.EAST);

                    // Add the recipe card to the panel
                    recipePanel.add(recipeCard);
                }

                // Update the UI
                SwingUtilities.invokeLater(() -> {
                    recipePanel.revalidate();
                    recipePanel.repaint();
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(RecipeGeneratorGUI.this, "Error fetching recipes. Please try again.");
                    progressBar.setVisible(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchRecipeDetails(int recipeId) {
        new Thread(() -> {
            try {
                // Construct the API URL for recipe details using URI
                URI uri = new URI(String.format(RECIPE_INFO_URL, recipeId) + "?apiKey=" + API_KEY);
                URL url = uri.toURL();

                // Open a connection to the API
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONObject recipeDetails = new JSONObject(response.toString());
                String title = recipeDetails.getString("title");
                String instructions = recipeDetails.getString("instructions");
                String imageUrl = recipeDetails.getString("image");

                // Display the details in a dialog
                SwingUtilities.invokeLater(() -> {
                    JTextArea detailsArea = new JTextArea();
                    detailsArea.setText("Title: " + title + "\n\nInstructions:\n" + instructions);
                    detailsArea.setEditable(false);
                    JOptionPane.showMessageDialog(RecipeGeneratorGUI.this, new JScrollPane(detailsArea), "Recipe Details", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(RecipeGeneratorGUI.this, "Error fetching recipe details.");
                });
                e.printStackTrace();
            }
        }).start();
    }


}
