package com.transnacala.lory.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.transnacala.lory.data.local.dao.CadeiraDao;
import com.transnacala.lory.data.local.dao.EnqueteDao;
import com.transnacala.lory.data.local.dao.EventoDao;
import com.transnacala.lory.data.local.dao.GrupoDao;
import com.transnacala.lory.data.local.dao.UtilizadorDao;
import com.transnacala.lory.data.local.dao.VotoLocalDao;
import com.transnacala.lory.data.local.entity.CadeiraEntity;
import com.transnacala.lory.data.local.entity.EnqueteEntity;
import com.transnacala.lory.data.local.entity.EventoEntity;
import com.transnacala.lory.data.local.entity.GrupoEntity;
import com.transnacala.lory.data.local.entity.OpcaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.QuestaoEnqueteEntity;
import com.transnacala.lory.data.local.entity.UtilizadorEntity;
import com.transnacala.lory.data.local.entity.VotoLocalEntity;

@Database(entities = {
        EventoEntity.class,
        CadeiraEntity.class,
        EnqueteEntity.class,
        OpcaoEnqueteEntity.class,
        QuestaoEnqueteEntity.class,
        GrupoEntity.class,
        UtilizadorEntity.class,
        VotoLocalEntity.class
}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS votos_locais (" +
                    "enqueteId TEXT NOT NULL, " +
                    "estudanteId TEXT NOT NULL, " +
                    "opcaoId TEXT NOT NULL, " +
                    "syncStatus TEXT NOT NULL, " +
                    "PRIMARY KEY(enqueteId, estudanteId))");
        }
    };
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE cadeiras ADD COLUMN docenteNome TEXT");
            database.execSQL("ALTER TABLE votos_locais ADD COLUMN votedAt INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS questoes_enquete (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "enqueteId TEXT, " +
                    "pergunta TEXT, " +
                    "updatedAt INTEGER NOT NULL)");
            database.execSQL("INSERT INTO questoes_enquete (id, enqueteId, pergunta, updatedAt) " +
                    "SELECT 'legacy-question-' || id, id, pergunta, updatedAt FROM enquetes");
            database.execSQL("ALTER TABLE opcoes_enquete RENAME TO opcoes_enquete_v5");
            database.execSQL("CREATE TABLE opcoes_enquete (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "enqueteId TEXT, " +
                    "questaoId TEXT NOT NULL, " +
                    "texto TEXT, " +
                    "votosCount INTEGER NOT NULL)");
            database.execSQL("INSERT INTO opcoes_enquete (id, enqueteId, questaoId, texto, votosCount) " +
                    "SELECT id, enqueteId, 'legacy-question-' || enqueteId, texto, votosCount " +
                    "FROM opcoes_enquete_v5");
            database.execSQL("DROP TABLE opcoes_enquete_v5");

            database.execSQL("ALTER TABLE votos_locais RENAME TO votos_locais_v5");
            database.execSQL("CREATE TABLE votos_locais (" +
                    "questaoId TEXT NOT NULL, " +
                    "estudanteId TEXT NOT NULL, " +
                    "opcaoId TEXT NOT NULL, " +
                    "syncStatus TEXT NOT NULL, " +
                    "votedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(questaoId, estudanteId))");
            database.execSQL("INSERT INTO votos_locais (questaoId, estudanteId, opcaoId, syncStatus, votedAt) " +
                    "SELECT 'legacy-question-' || enqueteId, estudanteId, opcaoId, syncStatus, votedAt " +
                    "FROM votos_locais_v5");
            database.execSQL("DROP TABLE votos_locais_v5");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public abstract EventoDao eventoDao();
    public abstract CadeiraDao cadeiraDao();
    public abstract EnqueteDao enqueteDao();
    public abstract GrupoDao grupoDao();
    public abstract UtilizadorDao utilizadorDao();
    public abstract VotoLocalDao votoLocalDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "lory_local_db")
                                    .addMigrations(MIGRATION_3_4)
                                    .addMigrations(MIGRATION_4_5)
                                    .addMigrations(MIGRATION_5_6)
                                    .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}