import java.io.Serializable;

public class LinkInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String dst;
        private String from;
        private int weight;

        private boolean isRouter;

        public LinkInfo(String dst, String from, int weight, boolean isRouter) {
                setDst(dst);
                setFrom(from);
                setWeight(weight);
                setRouter(isRouter);
        }


        public String getDst() {
                return dst;
        }
        public void setDst(String dst) {
                this.dst = dst;
        }
        public String getFrom() {
                return from;
        }
        public void setFrom(String from) {
                this.from = from;
        }
        public int getWeight() {
                return weight;
        }
        public void setWeight(int weight) {
                this.weight = weight;
        }
        public boolean isRouter() {
                return isRouter;
        }
        public void setRouter(boolean isRouter) {
                this.isRouter = isRouter;
        }
}