/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo entity processor base
 * @copyright 2024-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id§
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.processor;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

import java.util.List;
import java.util.Locale;

public class FilterExpressionVisitor implements ExpressionVisitor<Object> {
    private final Entity currentEntity;

    public FilterExpressionVisitor(Entity currentEntity) {
        this.currentEntity = currentEntity;
    }

    @Override
    public Object visitMember(final Member member) throws ODataApplicationException {
        final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

        if (1 == uriResourceParts.size() &&
                uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty)
        {
            UriResourcePrimitiveProperty uriResourceProperty =
                    (UriResourcePrimitiveProperty) uriResourceParts.get(0);

            return currentEntity.getProperty(
                    uriResourceProperty.getProperty().getName()).getValue();
        } else {
            throw new ODataApplicationException("Only primitive properties are implemented in filter expressions",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public Object visitLiteral(Literal literal)
            throws ODataApplicationException
    {
        Object retVal = null;
        String literalAsString = literal.getText();

        if (literal.getType() instanceof EdmString) {
            String stringLiteral = "";

            if (literal.getText().length() > 2) {
                stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
            }

            retVal= stringLiteral;
        } else {
            try {
                retVal = Integer.parseInt(literalAsString);
            } catch (NumberFormatException e) {
                throw new ODataApplicationException("Only Edm.Int32 and Edm.String literals are implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
            }
        }

        return retVal;
    }

    @Override
    public Object visitUnaryOperator(UnaryOperatorKind operator, Object operand)
            throws ODataApplicationException
    {
        Object retVal = null;

        System.out.println(operator);

        if (UnaryOperatorKind.NOT == operator && operand instanceof Boolean) {
            retVal = !(Boolean) operand;
        } else if (UnaryOperatorKind.MINUS == operator && operand instanceof Integer) {
            retVal = -(Integer) operand;
        } else {
            throw new ODataApplicationException("Invalid type for unary operator",
                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind operator,
                                      Object left, Object right)
            throws ODataApplicationException
    {
        Object retVal = null;

        switch (operator) {
            case ADD: /* Falls through */
            case MOD: /* Falls through */
            case MUL: /* Falls through */
            case DIV: /* Falls through */
            case SUB: /* Falls through */
                retVal = evaluateArithmeticOperation(operator, left, right);
                break;
            case EQ: /* Falls through */
            case NE: /* Falls through */
            case GE: /* Falls through */
            case GT: /* Falls through */
            case LE: /* Falls through */
            case LT: /* Falls through */
                retVal = evaluateComparisonOperation(operator, left, right);
                break;
            case AND: /* Falls through */
            case OR:  /* Falls through */
                retVal = evaluateBooleanOperation(operator, left, right);
                break;
            default:
                throw new ODataApplicationException("Binary operation " + operator.name() + " is not implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    private Object evaluateBooleanOperation(BinaryOperatorKind operator,
                                            Object left, Object right)
            throws ODataApplicationException
    {
        Object retVal = null;

        if (left instanceof Boolean && right instanceof Boolean) {
            Boolean valueLeft = (Boolean) left;
            Boolean valueRight = (Boolean) right;

            switch (operator) {
                case AND: retVal = valueLeft && valueRight; break;
                case OR:  retVal = valueLeft || valueRight; break;
            }
        } else {
            throw new ODataApplicationException("Boolean operations need two numeric operands",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    private Object evaluateComparisonOperation(BinaryOperatorKind operator, Object left, Object right)
            throws ODataApplicationException
    {
        Object retVal = null;

        if (left.getClass().equals(right.getClass()) && left instanceof Comparable) {
            int result;

            if (left instanceof Integer) {
                result = ((Comparable<Integer>) (Integer) left).compareTo((Integer) right);
            } else if (left instanceof String) {
                result = ((Comparable<String>) (String) left).compareTo((String) right);
            } else if (left instanceof Boolean) {
                result = ((Comparable<Boolean>) (Boolean) left).compareTo((Boolean) right);
            } else {
                throw new ODataApplicationException("Class " + left.getClass().getCanonicalName() + " not expected",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }

            switch (operator) {
                case EQ: retVal = 0 == result; break;
                case NE: retVal = 0 != result; break;
                case GE: retVal = 0 <= result; break;
                case GT: retVal = 0 <  result; break;
                case LE: retVal = 0 >= result; break;
                case LT: retVal = 0 >  result; break;
            }
        } else {
            throw new ODataApplicationException("Comparison needs two equal types",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    private Object evaluateArithmeticOperation(BinaryOperatorKind operator,
                                               Object left, Object right)
            throws ODataApplicationException
    {
        Object retVal = null;

        if (left instanceof Integer && right instanceof Integer) {
            Integer valueLeft = (Integer) left;
            Integer valueRight = (Integer) right;

            /* Calculate the result value */
            switch (operator) {
                case ADD: retVal = valueLeft + valueRight; break;
                case SUB: retVal = valueLeft - valueRight; break;
                case MUL: retVal = valueLeft * valueRight; break;
                case DIV: retVal = valueLeft / valueRight; break;
                case MOD: retVal = valueLeft % valueRight; break;
            }
        } else {
            throw new ODataApplicationException("Arithmetic operations needs two numeric operands",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    @Override
    public Object visitMethodCall(MethodKind methodCall, List<Object> parameters)
            throws ODataApplicationException
    {
        Object retVal = null;

        /* Implement method calls */
        switch (methodCall) {
            case CONTAINS:
                if (parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
                    String valueParam1 = (String) parameters.get(0);
                    String valueParam2 = (String) parameters.get(1);

                    retVal = valueParam1.contains(valueParam2);
                } else {
                    throw new ODataApplicationException("Contains needs two parameters of type Edm.String",
                            HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
                };
                break;
            default:
                throw new ODataApplicationException("Method call " + methodCall + " not implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    @Override
    public Object visitTypeLiteral(EdmType type) throws ODataApplicationException {
        throw new ODataApplicationException("Type literals are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitAlias(String aliasName) throws ODataApplicationException {
        throw new ODataApplicationException("Aliases are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitEnum(EdmEnumType type, List<String> enumValues)
            throws ODataApplicationException
    {
        throw new ODataApplicationException("Enums are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind,
                                      Object o, List<Object> list)
            throws ODataApplicationException
    {
        throw new ODataApplicationException("Binary operators are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitLambdaExpression(String lambdaFunction, String lambdaVariable,
                                        Expression expression)
            throws ODataApplicationException
    {
        throw new ODataApplicationException("Lamdba expressions are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitLambdaReference(String variableName)
            throws ODataApplicationException
    {
        throw new ODataApplicationException("Lamdba references are not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }
}
