package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class EntityManagerHelper {

    private static final EntityManagerFactory emf;
    private static final ThreadLocal<EntityManager> threadLocal;

    static {
        try {
            System.out.println("=== Создание EntityManagerFactory ===");
            emf = Persistence.createEntityManagerFactory("LibraryPU");
            System.out.println("EntityManagerFactory создан успешно");
        } catch (Exception e) {
            System.err.println("Ошибка при создании EntityManagerFactory: " + e.getMessage());
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
        threadLocal = new ThreadLocal<>();
    }

    public static EntityManager getEntityManager() {
        EntityManager em = threadLocal.get();
        if (em == null || !em.isOpen()) {
            em = emf.createEntityManager();
            threadLocal.set(em);
            System.out.println("Создан новый EntityManager");
        }
        return em;
    }

    public static void closeEntityManager() {
        EntityManager em = threadLocal.get();
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            System.out.println("EntityManager закрыт");
        }
        threadLocal.remove();
    }

    public static void beginTransaction() {
        EntityManager em = getEntityManager();
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            System.out.println("Транзакция начата");
        }
    }

    public static void commitTransaction() {
        EntityManager em = getEntityManager();
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
                System.out.println("Транзакция зафиксирована");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при фиксации транзакции: " + e.getMessage());
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    public static void rollbackTransaction() {
        EntityManager em = getEntityManager();
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
            System.out.println("Транзакция откатана");
        }
    }
}