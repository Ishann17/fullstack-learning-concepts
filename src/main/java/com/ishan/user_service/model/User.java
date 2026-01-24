package com.ishan.user_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @TableGenerator(
            name = "user_id_gen",
            table = "id_generator",
            pkColumnName = "gen_name",
            valueColumnName = "gen_value",
            pkColumnValue = "user_id",
            allocationSize = 1000
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_id_gen")
    private Integer id;
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String city;
    private String state;
    private int age;
    private String mobileNumber;
    private String gender;

}
/**
 * ✅ WHY we changed ID generation from IDENTITY -> TABLE (MySQL + bulk insert optimization)
 *
 * PROBLEM WITH GenerationType.IDENTITY (what we had earlier):
 * - In IDENTITY, the database generates the ID at insert time (AUTO_INCREMENT).
 * - Hibernate MUST insert the row immediately to get the generated ID back.
 * - Because of that, Hibernate cannot batch inserts properly.
 *
 * RESULT:
 * - Even if we set: hibernate.jdbc.batch_size=1000
 * - Hibernate still executes 1 INSERT per row (no JDBC batching)
 * - This is why bulk insert was slow and Hibernate stats showed:
 *   "executing 0 JDBC batches"
 *
 * ✅ WHY TABLE strategy helps:
 * - TABLE strategy uses a separate table to generate IDs.
 * - Hibernate can pre-allocate IDs in bulk BEFORE inserting rows.
 * - Example: allocationSize=1000
 *   -> Hibernate asks once: "Give me next 1000 IDs"
 *   -> Then it can insert 1000 users in a batch without waiting for DB IDs per row.
 *
 * ✅ WHAT TableGenerator does here:
 * - Creates/uses a table: id_generator
 * - Stores last used ID for different entities (gen_name -> gen_value)
 * - For User, we use key "user_id"
 *
 * ✅ PERFORMANCE BENEFIT:
 * - Enables true JDBC batching in MySQL
 * - Faster bulk inserts (less DB round-trips)
 * - Lower memory pressure + better throughput when inserting millions of records
 *
 * ⚠️ TRADE-OFFS / Notes:
 * - IDs can have gaps (normal in batch allocation).
 * - Requires the id_generator table to exist/manage values.
 * - TABLE is not as fast as SEQUENCE (Postgres), but in MySQL it's the best batching-friendly option.
 */