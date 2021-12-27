package pl.pijok.autosell.miner;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.pijok.autosell.AutoSell;
import pl.pijok.autosell.Controllers;
import pl.pijok.autosell.database.PreparedStatements;
import pl.pijok.autosell.essentials.Debug;
import pl.pijok.autosell.settings.Settings;
import pl.pijok.autosell.essentials.ConfigUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class MinersController {

    private HashMap<String, Miner> miners;

    public MinersController(){
        miners = new HashMap<>();
    }

    public void loadAllMinersData(){
        if(Settings.isDatabaseUsage()){
            //TODO LOADING DATA FROM DATABASE
            Bukkit.getScheduler().runTaskAsynchronously(AutoSell.getInstance(), new Runnable() {
                @Override
                public void run() {
                    try(Connection connection = Controllers.getDatabaseManager().getHikariDataSource().getConnection()){
                        PreparedStatement getAll = connection.prepareStatement(PreparedStatements.getAllPlayers);

                        ResultSet resultSet = getAll.executeQuery();

                        while(resultSet.next()){
                            String nickname = resultSet.getString("name");
                            long blocksMined = resultSet.getLong("blocksMined");
                            boolean autoSellEnabled = resultSet.getBoolean("autosell");

                            miners.put(nickname, new Miner(blocksMined, autoSellEnabled));
                        }

                        resultSet.close();

                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        Debug.log("&cCouldn't connect to database!");
                    }
                }
            });
        }
        else{
            YamlConfiguration configuration = ConfigUtils.load("players.yml");

            for(String nickname : configuration.getConfigurationSection("players").getKeys(false)){

                long blocksMined = configuration.getInt("players." + nickname + ".blocksMined");
                boolean autoSell = configuration.getBoolean("players." + nickname + ".autoSell");

                miners.put(nickname, new Miner(blocksMined, autoSell));
            }
        }
    }

    public void loadMiner(String nickname){
        if(Settings.isDatabaseUsage()){
            //TODO LOADING DATA FROM DATABASE
            Bukkit.getScheduler().runTaskAsynchronously(AutoSell.getInstance(), new Runnable() {
                @Override
                public void run() {
                    try(Connection connection = Controllers.getDatabaseManager().getHikariDataSource().getConnection()) {
                        PreparedStatement getMiner = connection.prepareStatement(PreparedStatements.getPlayer);
                        getMiner.setString(1, nickname);

                        long blocksMined = 0;
                        boolean autosell = false;

                        ResultSet resultSet = getMiner.executeQuery();
                        if(resultSet.getFetchSize() == 1){
                            blocksMined = resultSet.getInt("blocksMined");
                            autosell = resultSet.getBoolean("autosell");

                            getMiner.close();
                        }
                        else{
                            PreparedStatement insertMiner = connection.prepareStatement(PreparedStatements.insertPlayer);
                            insertMiner.setString(1, nickname);
                            insertMiner.setInt(2, 0);
                            insertMiner.setBoolean(3, false);
                            insertMiner.execute();

                            insertMiner.close();
                        }

                        resultSet.close();
                        miners.put(nickname, new Miner(blocksMined, autosell));

                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            });
        }
        else{
            YamlConfiguration configuration = ConfigUtils.load("players.yml");

            long blocksMined = 0;
            boolean autoSell = false;

            if(configuration.contains("players." + nickname)){
                blocksMined = configuration.getLong("players." + nickname + ".blocksMined");
                autoSell = configuration.getBoolean("players." + nickname + ".autoSell");
            }
            miners.put(nickname, new Miner(blocksMined, autoSell));
        }
    }

    public void createNewMiner(Player player){
        miners.put(player.getName(), new Miner(0, false));
    }

    public void saveAllMinersData(){
        if(Settings.isDatabaseUsage()){
            //TODO SAVING DATA TO DATABASE
            Bukkit.getScheduler().runTaskAsynchronously(AutoSell.getInstance(), new Runnable() {
                @Override
                public void run() {
                    try(Connection connection = Controllers.getDatabaseManager().getHikariDataSource().getConnection()) {
                        for(String nickname : miners.keySet()){
                            Miner miner = miners.get(nickname);
                            PreparedStatement statement = connection.prepareStatement(PreparedStatements.updatePlayer);
                            statement.setLong(1, miner.getMinedBlocks());
                            statement.setBoolean(2, miner.isAutoSell());
                            statement.setString(3, nickname);
                            statement.execute();

                            statement.close();
                        }

                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            });

        }
        else{
            YamlConfiguration configuration = ConfigUtils.load("players.yml");

            for(String nickname : miners.keySet()){
                Miner miner = miners.get(nickname);

                configuration.set("players." + nickname + ".blocksMined", miner.getMinedBlocks());
                configuration.set("players." + nickname + ".autoSell", miner.isAutoSell());
            }

            ConfigUtils.save(configuration, "players.yml");
        }

        miners.clear();
    }

    public void saveMiner(String nickname){
        if(Settings.isDatabaseUsage()) {
            //TODO SAVING DATA TO DATABASE
            try(Connection connection = Controllers.getDatabaseManager().getHikariDataSource().getConnection()){
                Miner miner = miners.get(nickname);
                PreparedStatement statement = connection.prepareStatement(PreparedStatements.updatePlayer);
                statement.setLong(1, miner.getMinedBlocks());
                statement.setBoolean(2, miner.isAutoSell());
                statement.setString(3, nickname);
                statement.execute();

                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        else{
            YamlConfiguration configuration = ConfigUtils.load("players.yml");

            Miner miner = miners.get(nickname);

            configuration.set("players." + nickname + ".blocksMined", miner.getMinedBlocks());
            configuration.set("players." + nickname + ".autoSell", miner.isAutoSell());

            ConfigUtils.save(configuration, "players.yml");
        }

        miners.remove(nickname);
    }

    public Miner getMiner(Player player){
        return miners.get(player.getName());
    }

    public Miner getMiner(String nickname){
        return miners.get(nickname);
    }

}
