package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description Block
 */

public class Block {
    private ArrayList<BlockItem> blockItems;
    private int endLineNum;

    public Block() {
        blockItems = new ArrayList<>();
        endLineNum = 0;
    }

    public ArrayList<BlockItem> getBlockItems() {
        return blockItems;
    }

    public int getEndLineNum() {
        return endLineNum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <Block> begin\n");
        sb.append("LBRACE {\n");
        for (BlockItem blockItem : blockItems) {
            sb.append(blockItem.toString());
        }
        sb.append("RBRACE }\n");
        sb.append("<Block>\n");
        return sb.toString();
    }

    public void parse() {
        // 读取左大括号
        Lexer.nextToken();
        Token token = Lexer.nextToken();
        while (!token.isRbrace()) {
            Lexer.rollback(1);
            BlockItem blockItem = new BlockItem();
            blockItem.parse();
            blockItems.add(blockItem);
            token = Lexer.nextToken();
        }
        endLineNum = token.getLineNum();
    }
}
