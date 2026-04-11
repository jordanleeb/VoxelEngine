package voxelengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Dissector {
    private HashMap<Long, Rect> tl, tr, bl, br, tc, bc;
    private HashMap<Integer, HashSet<Integer>> byTop, byBot;
    private HashMap<Integer, Rect> active;
    private ArrayDeque<Integer> queue;
    private int nextId;
    
    public Dissector() {
        tl = new HashMap<>();
        tr = new HashMap<>();
        bl = new HashMap<>();
        br = new HashMap<>();
        tc = new HashMap<>();
        bc = new HashMap<>();
        byTop = new HashMap<>();
        byBot = new HashMap<>();
        active = new HashMap<>();
        queue = new ArrayDeque<>();
        nextId = 0;
    }
    
    private long key2(int a, int b) {
        return (long) a << 32 | (b & 0xFFFFFFFFL);
    }

    private long key3(int a, int b, int c) {
        return (long) a * 1_000_000_000L + (long) b * 100_000L + c;
    }
    
    private void addRect(int t, int l, int b, int r) {
        Rect rect = new Rect(t, l, b, r);
        rect.id = nextId++;
        active.put(rect.id, rect);

        tl.put(key2(t, l), rect);
        tr.put(key2(t, r), rect);
        bl.put(key2(b, l), rect);
        br.put(key2(b, r), rect);
        tc.put(key3(t, l, r), rect);
        bc.put(key3(b, l, r), rect);

        byTop.computeIfAbsent(t, k -> new HashSet<>()).add(rect.id);
        byBot.computeIfAbsent(b, k -> new HashSet<>()).add(rect.id);

        queue.add(rect.id);
    }
    
    private void removeRect(Rect rect) {
        active.remove(rect.id);

        tl.remove(key2(rect.top, rect.left));
        tr.remove(key2(rect.top, rect.right));
        bl.remove(key2(rect.bottom, rect.left));
        br.remove(key2(rect.bottom, rect.right));
        tc.remove(key3(rect.top, rect.left, rect.right));
        bc.remove(key3(rect.bottom, rect.left, rect.right));

        HashSet<Integer> ts = byTop.get(rect.top);
        if (ts != null) {
            ts.remove(rect.id);
        }
        HashSet<Integer> bs = byBot.get(rect.bottom);
        if (bs != null) {
            bs.remove(rect.id);
        }
    }
    
    private boolean doHopper(Rect above, Rect below) {
        Rect n, w;
        if (above.getWidth() <= below.getWidth()) {
            n = above;
            w = below;
        } else {
            n = below;
            w = above;
        }
        if (n.left < w.left || n.right > w.right) {
            return false;
        }
        if (n.left != w.left && n.right != w.right) {
            return false;
        }

        removeRect(above);
        removeRect(below);
        addRect(above.top, n.left, below.bottom, n.right);
        if (n.left > w.left) {
            addRect(w.top, w.left, w.bottom, n.left - 1);
        }
        if (n.right < w.right) {
            addRect(w.top, n.right + 1, w.bottom, w.right);
        }
        return true;
    }
    
    private boolean tryHopper(Rect a) {
        Rect b;
        b = tl.get(key2(a.bottom + 1, a.left));
        if (b != null && active.containsKey(b.id) && doHopper(a, b)) {
            return true;
        }
        b = tr.get(key2(a.bottom + 1, a.right));
        if (b != null && active.containsKey(b.id) && doHopper(a, b)) {
            return true;
        }
        b = bl.get(key2(a.top - 1, a.left));
        if (b != null && active.containsKey(b.id) && doHopper(b, a)) {
            return true;
        }
        b = br.get(key2(a.top - 1, a.right));
        if (b != null && active.containsKey(b.id) && doHopper(b, a)) {
            return true;
        }
        return false;
    }
    
    private boolean doInvHopper(Rect ra, Rect rm, Rect rb) {
        removeRect(ra);
        removeRect(rm);
        removeRect(rb);
        addRect(ra.top, ra.left, rb.bottom, ra.right);
        if (ra.left > rm.left) {
            addRect(rm.top, rm.left, rm.bottom, ra.left - 1);
        }
        if (ra.right < rm.right) {
            addRect(rm.top, ra.right + 1, rm.bottom, rm.right);
        }
        return true;
    }

    private boolean tryInvHopper(Rect r) {
        // r as middle rect
        HashSet<Integer> topSet = byBot.get(r.top - 1);
        if (topSet != null) {
            for (int raId : new ArrayList<>(topSet)) {
                Rect ra = active.get(raId);
                if (ra != null && r.left <= ra.left && ra.right <= r.right) {
                    Rect rb = tc.get(key3(r.bottom + 1, ra.left, ra.right));
                    if (rb != null && active.containsKey(rb.id)) {
                        return doInvHopper(ra, r, rb);
                    }
                }
            }
        }

        // r as top outer rect
        HashSet<Integer> midTopSet = byTop.get(r.bottom + 1);
        if (midTopSet != null) {
            for (int rmId : new ArrayList<>(midTopSet)) {
                Rect rm = active.get(rmId);
                if (rm != null && rm.left <= r.left && r.right <= rm.right) {
                    Rect rb = tc.get(key3(rm.bottom + 1, r.left, r.right));
                    if (rb != null && active.containsKey(rb.id)) {
                        return doInvHopper(r, rm, rb);
                    }
                }
            }
        }

        // r as bottom outer rect
        HashSet<Integer> midBotSet = byBot.get(r.top - 1);
        if (midBotSet != null) {
            for (int rmId : new ArrayList<>(midBotSet)) {
                Rect rm = active.get(rmId);
                if (rm != null && rm.left <= r.left && r.right <= rm.right) {
                    Rect ra = bc.get(key3(rm.top - 1, r.left, r.right));
                    if (ra != null && active.containsKey(ra.id)) {
                        return doInvHopper(ra, rm, r);
                    }
                }
            }
        }

        return false;
    }
    
    private void seed(boolean[][] matrix) {
        for (int r = 0; r < matrix.length; r++) {
            int c = 0;
            while (c < matrix[r].length) {
                if (matrix[r][c]) {
                    int c2 = c;
                    while (c2 + 1 < matrix[r].length && matrix[r][c2 + 1]) {
                        c2++;
                    }
                    addRect(r, c, r, c2);
                    c = c2 + 1;
                } else {
                    c++;
                }
            }
        }
    }
    
    public static List<Rect> solve(boolean[][] matrix) {
        Dissector d = new Dissector();
        d.seed(matrix);

        while (!d.queue.isEmpty()) {
            int rid = d.queue.poll();
            Rect rect = d.active.get(rid);
            if (rect == null) {
                continue;
            }
            if (!d.tryHopper(rect)) {
                d.tryInvHopper(rect);
            }
        }

        return new ArrayList<>(d.active.values());
    }
}
