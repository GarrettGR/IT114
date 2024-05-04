package Project.common;

public enum PieceType {
  EMPTY, SHIP, HIT, MISS;

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String SQUARE = "\u25A0";

  @Override
  public String toString() {
      return switch (this) {
          case EMPTY -> " ";
          case SHIP -> ANSI_RESET + SQUARE;
          case HIT -> ANSI_RESET + ANSI_RED + SQUARE + ANSI_RESET;
          case MISS -> ANSI_RESET + ANSI_YELLOW + SQUARE + ANSI_RESET;
          default -> "?";
      };
  }
}