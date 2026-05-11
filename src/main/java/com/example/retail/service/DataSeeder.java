package com.example.retail.service;

import com.example.retail.domain.*;
import com.example.retail.repository.OrderRepository;
import com.example.retail.repository.ProductRepository;
import com.example.retail.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final ResourceLoader resourceLoader;

    @Value("${retail.seed.products-csv}")
    private String productsCsv;

    @Value("${retail.seed.users:8}")
    private int userCount;

    @Value("${retail.seed.orders-per-user-min:3}")
    private int ordersMin;

    @Value("${retail.seed.orders-per-user-max:8}")
    private int ordersMax;

    public DataSeeder(ProductRepository productRepo, UserRepository userRepo,
                      OrderRepository orderRepo, ResourceLoader resourceLoader) {
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        loadProducts();
        List<User> users = seedUsers();
        seedOrders(users);
    }

    private void loadProducts() throws Exception {
        Resource res = resourceLoader.getResource(productsCsv);
        if (!res.exists()) {
            throw new IllegalStateException("Seed CSV not found: " + productsCsv);
        }
        long t0 = System.currentTimeMillis();
        List<Product> batch = new ArrayList<>(1000);
        int total = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) throw new IllegalStateException("Empty CSV");
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                Product p = parseLine(line);
                batch.add(p);
                if (batch.size() >= 1000) {
                    productRepo.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            productRepo.saveAll(batch);
            total += batch.size();
        }
        log.info("Loaded {} products from {} in {} ms", total, productsCsv, System.currentTimeMillis() - t0);
    }

    /**
     * CSV parser tolerant of quoted fields with embedded commas.
     * Hand-rolled rather than pulling in opencsv for one file.
     */
    private Product parseLine(String line) {
        List<String> fields = new ArrayList<>(8);
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"' && cur.length() == 0) {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        fields.add(cur.toString());

        Product p = new Product();
        p.setSku(fields.get(0));
        p.setName(fields.get(1));
        p.setDescription(fields.get(2));
        p.setCategory(fields.get(3));
        p.setPrice(new BigDecimal(fields.get(4)));
        p.setStock(Integer.parseInt(fields.get(5)));
        p.setRating(Double.parseDouble(fields.get(6)));
        p.setImageUrl(fields.get(7));
        return p;
    }

    private List<User> seedUsers() {
        record Seed(String email, String name, String address) {}
        List<Seed> seeds = List.of(
            new Seed("alice@example.com",   "Alice Anderson",  "742 Evergreen Terrace, Springfield, OR 97477"),
            new Seed("bob@example.com",     "Bob Bennett",     "1600 Pennsylvania Ave NW, Washington, DC 20500"),
            new Seed("carol@example.com",   "Carol Chen",      "350 Fifth Ave, New York, NY 10118"),
            new Seed("david@example.com",   "David Diaz",      "1 Infinite Loop, Cupertino, CA 95014"),
            new Seed("eva@example.com",     "Eva Engström",    "221B Baker Street, London NW1 6XE"),
            new Seed("frank@example.com",   "Frank Fitzgerald","500 Terry A Francois Blvd, San Francisco, CA 94158"),
            new Seed("grace@example.com",   "Grace Goldberg",  "10 Downing Street, London SW1A 2AA"),
            new Seed("henry@example.com",   "Henry Hughes",    "1313 Mockingbird Ln, Mockingbird Heights, CA 91775")
        );
        List<User> users = new ArrayList<>();
        for (int i = 0; i < Math.min(userCount, seeds.size()); i++) {
            Seed s = seeds.get(i);
            User u = new User();
            u.setEmail(s.email);
            u.setFullName(s.name);
            u.setShippingAddress(s.address);
            u.setCreatedAt(Instant.now().minus(60 - i * 5, ChronoUnit.DAYS));
            users.add(userRepo.save(u));
        }
        log.info("Seeded {} users", users.size());
        return users;
    }

    private void seedOrders(List<User> users) {
        Random rng = new Random(7);
        long productCount = productRepo.count();
        if (productCount == 0) {
            log.warn("No products loaded — skipping order seed");
            return;
        }
        int totalOrders = 0;
        for (User u : users) {
            int n = ordersMin + rng.nextInt(ordersMax - ordersMin + 1);
            for (int i = 0; i < n; i++) {
                Order o = new Order();
                o.setUserId(u.getId());
                long ageDays = 1 + rng.nextInt(180);
                o.setPlacedAt(Instant.now().minus(ageDays, ChronoUnit.DAYS));
                o.setShippingAddress(u.getShippingAddress());
                o.setStatus(pickStatus(rng, ageDays));

                int lineCount = 1 + rng.nextInt(4);
                BigDecimal total = BigDecimal.ZERO;
                for (int j = 0; j < lineCount; j++) {
                    long pid = 1 + (long) rng.nextInt((int) productCount);
                    Product p = productRepo.findById(pid).orElse(null);
                    if (p == null) continue;
                    int qty = 1 + rng.nextInt(3);
                    OrderItem oi = new OrderItem();
                    oi.setOrder(o);
                    oi.setProductId(p.getId());
                    oi.setProductName(p.getName());
                    oi.setQuantity(qty);
                    oi.setUnitPrice(p.getPrice());
                    o.getItems().add(oi);
                    total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
                }
                if (o.getItems().isEmpty()) continue;
                o.setTotalAmount(total);
                orderRepo.save(o);
                totalOrders++;
            }
        }
        log.info("Seeded {} historical orders across {} users", totalOrders, users.size());
    }

    private OrderStatus pickStatus(Random rng, long ageDays) {
        if (ageDays > 30) return OrderStatus.DELIVERED;
        if (ageDays > 10) return rng.nextInt(10) == 0 ? OrderStatus.CANCELLED : OrderStatus.DELIVERED;
        if (ageDays > 3) return OrderStatus.SHIPPED;
        return rng.nextInt(3) == 0 ? OrderStatus.PENDING : OrderStatus.CONFIRMED;
    }
}
