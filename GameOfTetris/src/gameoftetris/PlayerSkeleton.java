import java.util.Arrays;

/**
 * This class is an implementation of a goal based Tetris playing agent. It
 * makes use of 2-layer local search to determine the best move to make next
 * given the current state (defined by the falling piece and the blocks already
 * placed on the board). The agent makes use of a heuristic function to
 * determine which states are better than others. This heuristic function makes
 * use of the following features: 1. The number of holes present 2. The number
 * of rows cleared so far 3. The maximum column height 4. The mean height
 * difference of every column 5. The sum of adjacent height variances 6. The sum
 * of pits 7. Is the state a lost state or not In order to determine the weights
 * to be given to each of these features, we ran the AI through a genetic
 * algorithm-based trainer, treating the set of seven weights as one chromosome
 * (with each allele corresponding to one of the seven weights) and the total
 * number of lines cleared until losing as the fitness function for the
 * chromosomes. After many evolutions on a population of 100 random chromosomes,
 * the chromosome with the best results was used as the weights for the
 * features.
 *
 */
public class PlayerSkeleton {

    public static double NUM_HOLES_WEIGHT;
    public static double COMPLETE_LINES_WEIGHT;
    public static double HEIGHT_VAR_WEIGHT;
    public static double LOST_WEIGHT;
    public static double MAX_HEIGHT_WEIGHT;
    public static double PIT_DEPTH_WEIGHT;
    public static double MEAN_HEIGHT_DIFF_WEIGHT;

    // Essentially the same as State.java. Reproduced here so that we can carry
    // out our local search across all possible resulting states given the
    // current state.
    public static class TestState {
        int[][] field;
        int[] top;
        int turn;
        int rowsCleared;
        boolean lost = false;

        public TestState(State s) {
            this.field = cloneField(s.getField());
            this.top = Arrays.copyOf(s.getTop(), s.getTop().length);
            this.turn = s.getTurnNumber();
            this.rowsCleared = s.getRowsCleared();
        }

        public TestState(TestState s) {
            this.field = cloneField(s.field);
            this.top = Arrays.copyOf(s.top, s.top.length);
            this.turn = s.turn;
            this.rowsCleared = s.rowsCleared;
        }

        private int[][] cloneField(int[][] field) {
            int[][] newField = new int[field.length][];
            for (int i = 0; i < newField.length; i++) {
                newField[i] = Arrays.copyOf(field[i], field[i].length);
            }
            return newField;
        }

        // returns false if you lose - true otherwise
        public boolean makeMove(int piece, int orient, int slot) {
            // height if the first column makes contact
            int height = top[slot] - pBottom[piece][orient][0];
            // for each column beyond the first in the piece
            for (int c = 1; c < pWidth[piece][orient]; c++) {
                height = Math.max(height, top[slot + c] - pBottom[piece][orient][c]);
            }

            // check if game ended
            if (height + pHeight[piece][orient] >= ROWS) {
                lost = true;
                return false;
            }

            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[piece][orient]; i++) {

                // from bottom to top of brick
                for (int h = height + pBottom[piece][orient][i]; h < height + pTop[piece][orient][i]; h++) {
                    field[h][i + slot] = turn;
                }
            }

            // adjust top
            for (int c = 0; c < pWidth[piece][orient]; c++) {
                top[slot + c] = height + pTop[piece][orient][c];
            }

            // check for full rows - starting at the top
            for (int r = height + pHeight[piece][orient] - 1; r >= height; r--) {
                // check all columns in the row
                boolean full = true;
                for (int c = 0; c < COLS; c++) {
                    if (field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                // if the row was full - remove it and slide above stuff down
                if (full) {
                    rowsCleared++;
                    // for each column
                    for (int c = 0; c < COLS; c++) {

                        // slide down all bricks
                        for (int i = r; i < top[c]; i++) {
                            field[i][c] = field[i + 1][c];
                        }
                        // lower the top
                        top[c]--;
                        while (top[c] >= 1 && field[top[c] - 1][c] == 0)
                            top[c]--;
                    }
                }
            }
            return true;
        }

    }

    // implement this function to have a working system
    public int pickMove(State s, int[][] legalMoves) {
        double bestValueSoFar = -1;
        TestState bestStateSoFar = null;
        int bestMoveSoFar = 0;
        for (int i = 0; i < legalMoves.length; i++) {
            TestState state = new TestState(s);
            state.makeMove(s.nextPiece, legalMoves[i][ORIENT], legalMoves[i][SLOT]);

            double value = !state.lost ? evaluateState(state) : evaluateOneLevelLower(state);
            if (value > bestValueSoFar || bestStateSoFar == null) {
                bestStateSoFar = state;
                bestValueSoFar = value;
                bestMoveSoFar = i;
            }

        }
        return bestMoveSoFar;
    }

    // Evaluate the value of the given state by going one layer deeper.
    // Given the board position, for each of the N_PIECES of tetrominos,
    // consider all
    // possible placements and rotations, and find the highest heuristic value
    // of all these resultant states of the particular tetromino. Find the
    // average max heuristic value across all N_PIECES tetrominos: this will be
    // the evaluation value for the state
    private double evaluateState(TestState state) {
        double sumLowerLevel = 0;
        for (int i = 0; i < N_PIECES; i++) {
            double maxSoFar = Integer.MIN_VALUE;
            for (int j = 0; j < legalMoves[i].length; j++) {
                TestState lowerState = new TestState(state);
                lowerState.makeMove(i, legalMoves[i][j][ORIENT], legalMoves[i][j][SLOT]);
                maxSoFar = Math.max(maxSoFar, evaluateOneLevelLower(lowerState));

            }
            sumLowerLevel += maxSoFar;
        }

        return sumLowerLevel / N_PIECES;
    }

    // Evaluate the state given features to be tested and weights. Apply
    // heuristic function.
    private double evaluateOneLevelLower(TestState state) {

        double h =
            -numHoles(state) * NUM_HOLES_WEIGHT + numRowsCleared(state) * COMPLETE_LINES_WEIGHT
                + -heightVariationSum(state) * HEIGHT_VAR_WEIGHT + lostStateValue(state) * LOST_WEIGHT
                + -maxHeight(state) * MAX_HEIGHT_WEIGHT + -pitDepthValue(state) * PIT_DEPTH_WEIGHT
                + -meanHeightDiffValue(state) * MEAN_HEIGHT_DIFF_WEIGHT;
        return h;
    }

    /*
     * ===================== Features calculations =====================
     */

    // By default, set the lost state value as -10
    private int lostStateValue(TestState state) {
        return hasLost(state) ? -10 : 0;
    }

    // The highest column in the board
    private static int maxHeight(TestState s) {
        int[] top = s.top;
        int maxSoFar = -1;
        for (int i : top) {
            maxSoFar = Math.max(maxSoFar, i);
        }

        return maxSoFar;
    }

    // Holes are defined as all empty cells that are below the top of each
    // column.
    private static int numHoles(TestState s) {
        int[][] field = s.field;
        int sumHoles = 0;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < s.top[col] - 1; row++) {
                if (field[row][col] == 0) {
                    sumHoles++;
                }
            }
        }
        return sumHoles;
    }

    private static int numRowsCleared(TestState s) {
        return s.rowsCleared;
    }

    // summing up the differences of adjacent column heights
    private static int heightVariationSum(TestState s) {
        int[] top = s.top;
        int varSum = 0;
        for (int i = 0; i < top.length - 1; i++) {
            varSum += Math.abs(top[i] - top[i + 1]);
        }

        return varSum;
    }

    private static boolean hasLost(TestState s) {
        return s.lost;
    }

    // The sum of all pit depths. A pit is defined as the difference in height
    // between a column and its two adjacent columns, with a minimum difference
    // of 3.
    public double pitDepthValue(TestState s) {
        int[] top = s.top;
        int pitDepthSum = 0;

        int pitColHeight;
        int leftColHeight;
        int rightColHeight;

        // pit depth of first column
        pitColHeight = top[0];
        rightColHeight = top[1];
        int diff = rightColHeight - pitColHeight;
        if (diff > 2) {
            pitDepthSum += diff;
        }

        for (int col = 0; col < State.COLS - 2; col++) {
            leftColHeight = top[col];
            pitColHeight = top[col + 1];
            rightColHeight = top[col + 2];

            int leftDiff = leftColHeight - pitColHeight;
            int rightDiff = rightColHeight - pitColHeight;
            int minDiff = Math.min(leftDiff, rightDiff);
            if (minDiff > 2) {
                pitDepthSum += minDiff;
            }
        }

        // pit depth of last column
        pitColHeight = top[State.COLS - 1];
        leftColHeight = top[State.COLS - 2];
        diff = leftColHeight - pitColHeight;
        if (diff > 2) {
            pitDepthSum += diff;
        }

        return pitDepthSum;

    }

    // The mean height difference is the average of all height differences
    // between each adjacent columns
    public double meanHeightDiffValue(TestState s) {
        int[] top = s.top;

        int sum = 0;
        for (int height : top) {
            sum += height;
        }

        float meanHeight = (float) sum / top.length;

        float avgDiff = 0;
        for (int height : top) {
            avgDiff += Math.abs(meanHeight - height);
        }

        return avgDiff / top.length;
    }

    public static void main(String[] args) {

        State s = new State();
        // The optimal set of weights found after 20 evolutions
        double[] weights =
            {1.7851855342334024, 1.4138726176225629, 0.3567297944529728, 0.6249287636118577, 0.051962392158941606,
                0.52385888919136, 0.12090744319379954};
        PlayerSkeleton p = new PlayerSkeleton(weights);
        while (!s.lost) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            // System.out.println(s.getRowsCleared());
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }

    public PlayerSkeleton(double[] weights) {
        NUM_HOLES_WEIGHT = weights[0];
        COMPLETE_LINES_WEIGHT = weights[1];
        HEIGHT_VAR_WEIGHT = weights[2];
        LOST_WEIGHT = weights[3];
        MAX_HEIGHT_WEIGHT = weights[4];
        PIT_DEPTH_WEIGHT = weights[5];
        MEAN_HEIGHT_DIFF_WEIGHT = weights[6];

    }

    // This method is used to train the agent via a genetic algorithm
    public int run() {

        State s = new State();
        while (!s.lost) {
            s.makeMove(pickMove(s, s.legalMoves()));
            // if (s.getRowsCleared() % 100000 == 0) {
            // System.out.println(s.getRowsCleared());
            // }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");

        return s.getRowsCleared();
    }

    /*
     * =============== Random info copied from State.java ===============
     */
    public static final int COLS = State.COLS;
    public static final int ROWS = State.ROWS;
    public static final int N_PIECES = State.N_PIECES;
    // all legal moves - first index is piece type - then a list of 2-length
    // arrays
    protected static int[][][] legalMoves = new int[N_PIECES][][];

    // indices for legalMoves
    public static final int ORIENT = 0;
    public static final int SLOT = 1;

    // possible orientations for a given piece type
    protected static int[] pOrients = {1, 2, 4, 4, 4, 2, 2};

    // the next several arrays define the piece vocabulary in detail
    // width of the pieces [piece ID][orientation]
    protected static int[][] pWidth = { {2}, {1, 4}, {2, 3, 2, 3}, {2, 3, 2, 3}, {2, 3, 2, 3}, {3, 2}, {3, 2}};
    // height of the pieces [piece ID][orientation]
    private static int[][] pHeight = { {2}, // square
        {4, 1}, // vertical piece
        {3, 2, 3, 2}, // L
        {3, 2, 3, 2}, //
        {3, 2, 3, 2}, // T
        {2, 3}, {2, 3}};
    private static int[][][] pBottom = { {{0, 0}}, { {0}, {0, 0, 0, 0}},
        { {0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}}, // L,
        { {0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}}, { {0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}}, { {0, 0, 1}, {1, 0}},
        { {1, 0, 0}, {0, 1}}};
    private static int[][][] pTop = { {{2, 2}}, { {4}, {1, 1, 1, 1}}, { {3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
        { {1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}}, { {3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}}, { {1, 2, 2}, {3, 2}},
        { {2, 2, 1}, {2, 3}}};

    // initialize legalMoves
    // legalMoves[piece type][num legal moves][tuple of orient and slot]
    {
        // for each piece type
        for (int i = 0; i < N_PIECES; i++) {
            // figure number of legal moves
            int n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                // number of locations in this orientation
                n += COLS + 1 - pWidth[i][j];
            }
            // allocate space
            legalMoves[i] = new int[n][2];
            // for each orientation
            n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                // for each slot
                for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
                    legalMoves[i][n][ORIENT] = j;
                    legalMoves[i][n][SLOT] = k;
                    n++;
                }
            }
        }
    }

}
