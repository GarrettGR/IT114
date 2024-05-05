package Project.common;

import java.io.Serializable;

public class PlayerData implements Serializable{
    private int score ;
    private int health;
    private int hits;
    private int misses;
    private int currency;

    // Should include the GameBoard in the future...

    public PlayerData(int health) {
      this.score = 0;
      this.health = health;
      this.hits = 0;
      this.misses = 0;
      this.currency = 0;
    }

    public PlayerData(int score, int health, int hits, int misses, int currency) {
      this.score = score;
      this.health = health;
      this.hits = hits;
      this.misses = misses;
      this.currency = currency;
    }

    public PlayerData(PlayerData player) {
      this.score = player.getScore();
      this.health = player.getHealth();
      this.hits = player.getHits();
      this.misses = player.getMisses();
      this.currency = player.getCurrency();
    }

    // --- Scores ---

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

    public void incrementScore() { score++; }

    public void incrementScore(int points) { score += points; }

    // --- Health ---

    public int getHealth() { return health; }

    public void setHealth(int health) { this.health = health; }

    public void incrementHealth() { health++; }

    public void incrementHealth(int health) { this.health += health; }

    public void decrementHealth() { health--; }

    public void decrementHealth(int damage) { health -= damage; }

    // --- Hits ---

    public int getHits() { return hits; }

    public void setHits(int hits) { this.hits = hits; }

    public void incrementHits() { hits++; }

    // --- Misses ---

    public int getMisses() { return misses; }

    public void setMisses(int misses) { this.misses = misses; }

    public void incrementMisses() { misses++; }

    // --- Currency ---

    public int getCurrency() { return currency; }

    public void incrementCurrency() { currency++; }
    
    public void incrementCurrency(int amount) { currency += amount; }

    public void decrementCurrency() { currency--; }

    public void decrementCurrency(int amount) { currency -= amount; }

    public void setCurrency(int currency) { this.currency = currency; }

    @Override
    public String toString() {
      return String.format("%d points, %d health, %d hits, %d misses, %d currency", score, health, hits, misses, currency);
    }
}