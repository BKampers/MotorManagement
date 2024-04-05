package randd.motormanagement.swing;


import java.awt.*;


class CogwheelRenderer extends javax.swing.JPanel {
    

    void setCogCount(int numberOfCogs) {
        this.cogCount = numberOfCogs;
    }


    void setGapLength(int gapLength) {
        this.gapLength = gapLength;
    }


    void setDeadPoints(java.util.List<Integer> deadPoints) {
        this.deadPoints = deadPoints;
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.translate(LEFT_MARGIN, TOP_MARGIN);
        Dimension size = getSize();
        int outerDiameter = Math.min(size.width, size.height) - Y_OFFSET;
        int cogHeight = Math.round(outerDiameter * 0.1f);
        int innerDiameter = outerDiameter - cogHeight * 2;
        Point center = new Point(outerDiameter / 2, outerDiameter / 2);
        int cogTotal = cogCount + gapLength;
        int halfCogAngle = Math.round(360.0f / cogTotal / 2.0f);
        double cogRadians = (2 * Math.PI) / cogTotal;
        double halfCogRadians = cogRadians / 2;
        double angleOffset = 0.0;
        if (deadPoints != null && ! deadPoints.isEmpty()) {
            angleOffset = - halfCogRadians * ((2 * deadPoints.get(0)) - 1);
        }
        g2d.rotate(angleOffset, center.x, center.y);
        BasicStroke normalStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.0f}, 0);
        BasicStroke referenceStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {3.0f}, 0);
        for (int cog = 1; cog <= cogTotal; ++cog) {
            if (cog <= cogCount) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(normalStroke);
            }
            else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setStroke(referenceStroke);
            }
            boolean atDeadPoint = false;
            if (deadPoints != null) {
                for (int deadPoint : deadPoints) {
                    if (deadPoint == cog) {
                        atDeadPoint = true;
                    }
                }
            }
            g2d.drawLine(center.x, 0, center.x, cogHeight);
            g2d.drawArc(0, 0, outerDiameter, outerDiameter, 90, -halfCogAngle);
            g2d.rotate(halfCogRadians, center.x, center.y);
            Color save = g2d.getColor();
            if (atDeadPoint) {
                g2d.setColor(Color.YELLOW);
            }
            g2d.drawLine(center.x, 0, center.x, cogHeight);
            g2d.setColor(save);
            g2d.drawArc(cogHeight, cogHeight, innerDiameter, innerDiameter, 90, -halfCogAngle);
            g2d.rotate(halfCogRadians, center.x, center.y);
        }
        g2d.rotate(-angleOffset, center.x, center.y);
        g2d.setColor(Color.BLUE);
        if (deadPoints != null) {
            for (int i = 0; i < deadPoints.size(); i++) {
                int deadPoint = deadPoints.get(i);
                double angle = 2 * Math.PI * ((double) deadPoint - deadPoints.get(0)) / cogTotal;
                int radius = outerDiameter / 2 - 60;
                String string = deadPointString(deadPoint, (i == 0) ? "TDP" : "DP");
                FontMetrics metrics = g2d.getFontMetrics();
                Point point = new Point();
                point.x = center.x + Math.round((float) (Math.sin(angle) * radius)) - metrics.stringWidth(string) / 2;
                point.y = center.y - Math.round((float) (Math.cos(angle) * radius)) + metrics.getHeight() / 2;
                g2d.drawString(string, point.x, point.y);
            }
        }
    }
    
    
    private String deadPointString(int deadPoint, String key) {
        StringBuilder builder = new StringBuilder();
        builder.append(deadPoint);
        builder.append(" (");
        builder.append(Bundle.getInstance().get(key));
        builder.append(')');
        return builder.toString();
    }


    private int cogCount;
    private int gapLength;
    private java.util.List<Integer> deadPoints;

    private static final int Y_OFFSET = 15;

    private static final int LEFT_MARGIN = 5;
    private static final int TOP_MARGIN = 10;

    
}
