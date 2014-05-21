package org.infinispan.odata.producer;

import org.apache.log4j.Logger;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.odata4j.expression.AddExpression;
import org.odata4j.expression.AggregateAllFunction;
import org.odata4j.expression.AggregateAnyFunction;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BinaryLiteral;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.ByteLiteral;
import org.odata4j.expression.CastExpression;
import org.odata4j.expression.CeilingMethodCallExpression;
import org.odata4j.expression.ConcatMethodCallExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DayMethodCallExpression;
import org.odata4j.expression.DecimalLiteral;
import org.odata4j.expression.DivExpression;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EndsWithMethodCallExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.ExpressionVisitor;
import org.odata4j.expression.FloorMethodCallExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.GuidLiteral;
import org.odata4j.expression.HourMethodCallExpression;
import org.odata4j.expression.IndexOfMethodCallExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.IsofExpression;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LengthMethodCallExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.MinuteMethodCallExpression;
import org.odata4j.expression.ModExpression;
import org.odata4j.expression.MonthMethodCallExpression;
import org.odata4j.expression.MulExpression;
import org.odata4j.expression.NeExpression;
import org.odata4j.expression.NegateExpression;
import org.odata4j.expression.NotExpression;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.ParenExpression;
import org.odata4j.expression.ReplaceMethodCallExpression;
import org.odata4j.expression.RoundMethodCallExpression;
import org.odata4j.expression.SByteLiteral;
import org.odata4j.expression.SecondMethodCallExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StartsWithMethodCallExpression;
import org.odata4j.expression.StringLiteral;
import org.odata4j.expression.SubExpression;
import org.odata4j.expression.SubstringMethodCallExpression;
import org.odata4j.expression.SubstringOfMethodCallExpression;
import org.odata4j.expression.TimeLiteral;
import org.odata4j.expression.ToLowerMethodCallExpression;
import org.odata4j.expression.ToUpperMethodCallExpression;
import org.odata4j.expression.TrimMethodCallExpression;
import org.odata4j.expression.YearMethodCallExpression;

/**
 * Expression visitor capable of mapping OData queries (expressions) to Apache Lucene queries
 * used for querying Infinispan cache.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class MapQueryExpressionVisitor implements ExpressionVisitor {

    private static final Logger log = Logger.getLogger(MapQueryExpressionVisitor.class.getName());

    private Query tmpQuery;
    private QueryBuilder queryBuilder;

    public MapQueryExpressionVisitor(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public Query getBuiltLuceneQuery() {
        // tmpQuery has been built in the whole process of visiting expressions
        log.trace("From MapQueryExpressionVisitor: returning tmpQuery (to InfinispanProducer): " + tmpQuery);
        return (Query) tmpQuery;
    }

    /**
     * This method acts as a resolver for calling responsible visitor method.
     *
     * @param expr - general expression
     */
    public void visit(BoolCommonExpression expr) {
        if(expr.getClass().getInterfaces()[0] == AndExpression.class) {
            visit((AndExpression) expr);
        }
        if(expr.getClass().getInterfaces()[0] == OrExpression.class) {
            visit((OrExpression) expr);
        }
        if(expr.getClass().getInterfaces()[0] == EqExpression.class) {
            visit((EqExpression) expr);
        }
        log.trace("End of the main BoolCommonExpression -- actual value of tmpQuery: " + tmpQuery);
    }

    @Override
    public void visit(AndExpression expr) {
        BooleanQuery booleanQuery = new BooleanQuery();
        visit(expr.getLHS());
        booleanQuery.add(this.tmpQuery, BooleanClause.Occur.MUST);
        visit(expr.getRHS());
        booleanQuery.add(this.tmpQuery, BooleanClause.Occur.MUST);

        this.tmpQuery = booleanQuery;
        log.trace("End of AND expr -- tmpQuery set to: " + tmpQuery);
    }

    @Override
    public void visit(OrExpression expr) {
        BooleanQuery booleanQuery = new BooleanQuery();
        visit(expr.getLHS());
        booleanQuery.add(this.tmpQuery, BooleanClause.Occur.SHOULD);
        visit(expr.getRHS());
        booleanQuery.add(this.tmpQuery, BooleanClause.Occur.SHOULD);

        this.tmpQuery = booleanQuery;
        log.trace("End of OR expr -- tmpQuery set to: " + tmpQuery);
    }

    @Override
    public void visit(EqExpression expr) {

        EntitySimpleProperty espLhs = (EntitySimpleProperty) expr.getLHS();
        log.trace("eqExpression.getLHS() getPropertyName(): " + espLhs.getPropertyName());
        // Some other literals can be obtained here (support it in 1.1)
        StringLiteral slRhs = (StringLiteral) expr.getRHS();
        log.trace("eqExpression.getRHS() getValue(): " + slRhs.getValue());

        this.tmpQuery = this.queryBuilder.phrase()
                .onField(espLhs.getPropertyName())
                .sentence(slRhs.getValue())
                .createQuery();
        log.trace("End of EQ expr -- tmpQuery set to: " + tmpQuery);
    }

    @Override
    public void beforeDescend() {
        // TODO: Customise this generated block
    }

    @Override
    public void afterDescend() {
        // TODO: Customise this generated block
    }

    @Override
    public void betweenDescend() {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(String type) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(OrderByExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(OrderByExpression.Direction direction) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(AddExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(BooleanLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(CastExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ConcatMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DateTimeLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DateTimeOffsetLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DecimalLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DivExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(EndsWithMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(EntitySimpleProperty expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(GeExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(GtExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(GuidLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(BinaryLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ByteLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SByteLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(IndexOfMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SingleLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DoubleLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(IntegralLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(Int64Literal expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(IsofExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(LeExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(LengthMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(LtExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ModExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(MulExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(NeExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(NegateExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(NotExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(NullLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ParenExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(BoolParenExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ReplaceMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(StartsWithMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(StringLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SubExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SubstringMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SubstringOfMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(TimeLiteral expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ToLowerMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(ToUpperMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(TrimMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(YearMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(MonthMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(DayMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(HourMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(MinuteMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(SecondMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(RoundMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(FloorMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(CeilingMethodCallExpression expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(AggregateAnyFunction expr) {
        // TODO: Customise this generated block
    }

    @Override
    public void visit(AggregateAllFunction expr) {
        // TODO: Customise this generated block
    }
}
