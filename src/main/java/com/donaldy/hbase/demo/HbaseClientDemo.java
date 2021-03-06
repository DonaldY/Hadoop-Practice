package com.donaldy.hbase.demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author donald
 * @date 2020/08/29
 */
public class HbaseClientDemo {

    private Connection conn = null;
    private HBaseAdmin admin = null;

    @Before
    public void init () throws IOException {

        Configuration conf = HBaseConfiguration.create();

        conf.set("hbase.zookeeper.quorum", "172.16.64.121, 172.16.64.122");

        conf.set("hbase.zookeeper.property.clientPort", "2181");

        conn = ConnectionFactory.createConnection(conf);
    }

    /**
     * 创建表
     *
     * @throws IOException 异常
     */
    @Test
    public void createTable() throws IOException {

        admin = (HBaseAdmin) conn.getAdmin();

        // 创建表描述器
        HTableDescriptor teacher = new HTableDescriptor(TableName.valueOf("teacher"));

        // 设置列族描述器
        teacher.addFamily(new HColumnDescriptor("info"));

        // 执行创建操作
        admin.createTable(teacher);

        System.out.println("teacher表创建成功!!");
    }

    @Test
    public void putRelationData() throws IOException {

        Table table = conn.getTable(TableName.valueOf("relations"));

        List<Put> puts = new ArrayList<Put>();
        // rowkey  user1
        Put put = new Put(Bytes.toBytes("user1"));
        put.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("user2"),Bytes.toBytes("user2"));
        put.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("user3"),Bytes.toBytes("user3"));
        put.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("user4"),Bytes.toBytes("user4"));
        puts.add(put);

        // rowkey user2
        Put put2 = new Put(Bytes.toBytes("user2"));
        put.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("user1"),Bytes.toBytes("user1"));
        put.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("user3"),Bytes.toBytes("user3"));
        puts.add(put2);

        table.put(puts);

        table.close();
    }

    /**
     * 插入一条记录
     *
     * @throws IOException 异常
     */
    @Test
    public void putData() throws IOException {

        // 获取一个表对象
        Table t = conn.getTable(TableName.valueOf("teacher"));

        // 设定 rowkey
        Put put = new Put(Bytes.toBytes("110"));

        // 列族,列,value
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("addr"), Bytes.toBytes("beijing"));

        // 执行插入
        t.put(put);
        //可以传入list批量插入数据

        //关闭table对象
        t.close();

        System.out.println("插入成功!!");
    }

    /**
     * 删除数据
     * @throws IOException 异常
     */
    @Test
    public void deleteData() throws IOException {

        // 需要获取一个table对象
        final Table worker = conn.getTable(TableName.valueOf("teacher"));

        // 准备delete对象
        final Delete delete = new Delete(Bytes.toBytes("110"));

        // 执行删除
        worker.delete(delete);

        // 关闭table对象
        worker.close();

        System.out.println("删除数据成功!!");
    }

    /**
     * 删除指定rowkey中，某一列(column)
     *
     * @throws IOException 异常
     */
    @Test
    public void deleteData2() throws IOException {

        final Table worker = conn.getTable(TableName.valueOf("relations"));

        final Delete delete = new Delete(Bytes.toBytes("uid1"));

        // 获取 rowkey uid1
        System.out.println(new String(delete.getRow()));

        delete.addColumns(Bytes.toBytes("friends"), Bytes.toBytes("uid2"));

        // 获取 rowkey 中 第一个 列族 friends
        System.out.println(new String(delete.getFamilyCellMap().firstKey()));

        // 获取 rowkey的列族中的 column uid2
        Cell cell = delete.getFamilyCellMap().get(Bytes.toBytes("friends")).get(0);
        System.out.println(new String(cell.getQualifier()));

        // 执行删除
        worker.delete(delete);

        // 关闭table对象
        worker.close();

        System.out.println("删除数据成功!!");
    }

    @Test
    public void deleteData3() throws IOException {

        final Table worker = conn.getTable(TableName.valueOf("relations"));

        final Delete delete = new Delete(Bytes.toBytes("uid1"));

        delete.addColumns(Bytes.toBytes("friends"), Bytes.toBytes("uid2"));

        List<Cell> cells = delete.getFamilyCellMap().get(Bytes.toBytes("friends"));

        if (CollectionUtils.isEmpty(cells)) {

            worker.close();

            return;
        }

        // 获取 uid1 第一个 column
        Cell cell = cells.get(0);

        byte[] column = CellUtil.cloneQualifier(cell);
        byte[] rowKey = CellUtil.cloneRow(cell);

        System.out.println("column : " + new String(column));
        System.out.println("rowKey : " + new String(rowKey));

        // 创建 uid2， 并设置需要删除的 column
        Delete otherUserDelete = new Delete(column);

        otherUserDelete.addColumns(Bytes.toBytes("friends"), rowKey);

        worker.delete(otherUserDelete);

        // 关闭table对象
        worker.close();

        System.out.println("删除数据成功!!");
    }

    @Test
    public void getDataByCF() throws IOException {

        // 获取表对象
        HTable teacher = (HTable) conn.getTable(TableName.valueOf("teacher"));

        // 创建查询的get对象
        Get get = new Get(Bytes.toBytes("110"));

        // 指定列族信息
        get.addColumn(Bytes.toBytes("info"), Bytes.toBytes("sex"));

        get.addFamily(Bytes.toBytes("info"));

        //执行查询
        Result res = teacher.get(get);
        Cell[] cells = res.rawCells();

        // 获取改行的所有cell对象
        for (Cell cell : cells) {

            // 通过cell获取 rowkey, cf, column, value
            String cf = Bytes.toString(CellUtil.cloneFamily(cell));
            String column = Bytes.toString(CellUtil.cloneQualifier(cell));
            String value = Bytes.toString(CellUtil.cloneValue(cell));
            String rowKey = Bytes.toString(CellUtil.cloneRow(cell));
            System.out.println(rowKey + "----" + cf + "---" + column + "---" + value);
        }

        // 关闭表对象资源
        teacher.close();
    }

    /**
     * 全表扫描
     */
    @Test
    public void scanAllData() throws IOException {

        HTable teacher = (HTable) conn.getTable(TableName.valueOf("teacher"));

        Scan scan = new Scan();

        ResultScanner resultScanner = teacher.getScanner(scan);

        for (Result result : resultScanner) {
            Cell[] cells = result.rawCells();

            // 获取改行的所有cell对象
            for (Cell cell : cells) {
                // 通过cell获取rowkey,cf,column,value
                String cf = Bytes.toString(CellUtil.cloneFamily(cell));
                String column = Bytes.toString(CellUtil.cloneQualifier(cell));
                String value = Bytes.toString(CellUtil.cloneValue(cell));
                String rowkey = Bytes.toString(CellUtil.cloneRow(cell));
                System.out.println(rowkey + "----" + cf + "--" + column + "---" + value);
            }
        }
        teacher.close();
    }

    /**
     * 通过 startRowKey 和 endRowKey 进行扫描查询
     */
    @Test
    public void scanRowKey() throws IOException {

        HTable teacher = (HTable) conn.getTable(TableName.valueOf("teacher"));

        Scan scan = new Scan();

        scan.setStartRow("0001".getBytes());

        scan.setStopRow("2".getBytes());

        ResultScanner resultScanner = teacher.getScanner(scan);

        teacher.close();
    }

    @After
    public void destroy(){

        if(admin != null){
            try {
                admin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(conn != null){
            try {
                conn.close();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
}
