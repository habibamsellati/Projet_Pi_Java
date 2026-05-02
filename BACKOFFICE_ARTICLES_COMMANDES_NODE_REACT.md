# Backoffice Articles & Commandes (Node.js / React)

Ce document reprend et structure votre proposition de backoffice pour les modules **Articles** et **Commandes**.

## Contexte
- Stack proposée: **Node.js / Express + MongoDB (Mongoose) + React**
- Objectif: interface admin pour modération d'articles et gestion des commandes

---

## Module Articles

### Scénario fonctionnel
L'admin accède à la page Articles pour modérer le contenu publié. Il peut:
- rechercher un article,
- trier par date,
- voir/supprimer des commentaires,
- supprimer un article,
- ouvrir les statistiques globales.

### Backend — Routes (Express)
```js
// routes/admin/articles.js
const express = require('express');
const router = express.Router();
const Article = require('../../models/Article');
const Comment = require('../../models/Comment');
const authAdmin = require('../../middleware/authAdmin');

// GET /admin/articles — Liste avec recherche et tri
router.get('/', authAdmin, async (req, res) => {
  const { search = '', sort = 'desc' } = req.query;

  const query = search
    ? {
        $or: [
          { title: { $regex: search, $options: 'i' } },
          { content: { $regex: search, $options: 'i' } },
          { 'author.name': { $regex: search, $options: 'i' } },
        ],
      }
    : {};

  const articles = await Article.find(query)
    .sort({ createdAt: sort === 'asc' ? 1 : -1 })
    .populate('author', 'name role')
    .populate('comments');

  res.json({ articles });
});

// DELETE /admin/articles/:id — Supprimer un article
router.delete('/:id', authAdmin, async (req, res) => {
  await Comment.deleteMany({ article: req.params.id });
  await Article.findByIdAndDelete(req.params.id);
  res.json({ message: 'Article supprime' });
});

// DELETE /admin/articles/:id/comments/:commentId — Supprimer un commentaire
router.delete('/:id/comments/:commentId', authAdmin, async (req, res) => {
  await Comment.findByIdAndDelete(req.params.commentId);
  await Article.findByIdAndUpdate(req.params.id, {
    $pull: { comments: req.params.commentId },
  });
  res.json({ message: 'Commentaire supprime' });
});

// GET /admin/articles/stats — Statistiques
router.get('/stats', authAdmin, async (req, res) => {
  const totalArticles = await Article.countDocuments();
  const totalComments = await Comment.countDocuments();
  const recentArticles = await Article.countDocuments({
    createdAt: { $gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) },
  });
  res.json({ totalArticles, totalComments, recentArticles });
});

module.exports = router;
```

### Modèle Article (Mongoose)
```js
// models/Article.js
const mongoose = require('mongoose');

const articleSchema = new mongoose.Schema(
  {
    title:   { type: String, required: true },
    content: { type: String, required: true },
    author:  { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    comments:[{ type: mongoose.Schema.Types.ObjectId, ref: 'Comment' }],
    isValidated: { type: Boolean, default: false },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Article', articleSchema);
```

### Frontend — Page Admin Articles (React)
```jsx
// pages/admin/Articles.jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

export default function AdminArticles() {
  const [articles, setArticles] = useState([]);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('desc');

  const fetchArticles = async () => {
    const { data } = await axios.get('/admin/articles', {
      params: { search, sort },
    });
    setArticles(data.articles);
  };

  useEffect(() => { fetchArticles(); }, [sort]);

  const deleteArticle = async (id) => {
    if (!confirm('Supprimer cet article ?')) return;
    await axios.delete(`/admin/articles/${id}`);
    fetchArticles();
  };

  const deleteComment = async (articleId, commentId) => {
    await axios.delete(`/admin/articles/${articleId}/comments/${commentId}`);
    fetchArticles();
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Articles</h1>
      </div>

      <div className="flex gap-3 mb-6">
        <input
          type="text"
          placeholder="Rechercher par titre, contenu ou auteur..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input flex-1"
        />
        <button className="btn-primary" onClick={fetchArticles}>Rechercher</button>
        <select value={sort} onChange={(e) => setSort(e.target.value)} className="select">
          <option value="desc">Date (recentes)</option>
          <option value="asc">Date (anciennes)</option>
        </select>
      </div>

      {articles.map((article) => (
        <div key={article._id} className="card mb-4">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="font-semibold text-lg">{article.title}</h2>
              <p className="text-sm text-muted">
                {article.author?.name} · {new Date(article.createdAt).toLocaleDateString('fr-FR')}
              </p>
            </div>
            <button className="btn-danger" onClick={() => deleteArticle(article._id)}>
              Supprimer l'article
            </button>
          </div>

          <div className="mt-4 border-t pt-4">
            <p className="font-medium text-sm mb-2">Commentaires ({article.comments?.length || 0})</p>
            {article.comments?.length === 0 ? (
              <p className="text-sm text-muted">Aucun commentaire.</p>
            ) : (
              article.comments.map((comment) => (
                <div key={comment._id} className="flex justify-between items-center py-1">
                  <p className="text-sm">{comment.content}</p>
                  <button className="btn-danger-sm" onClick={() => deleteComment(article._id, comment._id)}>
                    Supprimer
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
```

---

## Module Commandes

### Scénario fonctionnel
L'admin voit toutes les commandes et peut:
- filtrer par statut,
- rechercher par client,
- valider/invalider une commande,
- exporter les commandes en PDF.

### Backend — Routes (Express)
```js
// routes/admin/commandes.js
const express = require('express');
const router = express.Router();
const Order = require('../../models/Order');
const PDFDocument = require('pdfkit');
const authAdmin = require('../../middleware/authAdmin');

router.get('/', authAdmin, async (req, res) => {
  const { search = '', status = '' } = req.query;

  const query = {};
  if (status) query.status = status;
  if (search) {
    query.$or = [
      { 'client.firstName': { $regex: search, $options: 'i' } },
      { 'client.lastName': { $regex: search, $options: 'i' } },
    ];
  }

  const orders = await Order.find(query)
    .sort({ createdAt: -1 })
    .populate('client', 'firstName lastName phone')
    .populate('items.article', 'title price');

  res.json({ orders });
});

router.patch('/:id/validate', authAdmin, async (req, res) => {
  const order = await Order.findByIdAndUpdate(
    req.params.id,
    { status: 'validee' },
    { new: true }
  );
  res.json({ order });
});

router.patch('/:id/invalidate', authAdmin, async (req, res) => {
  const order = await Order.findByIdAndUpdate(
    req.params.id,
    { status: 'invalidee' },
    { new: true }
  );
  res.json({ order });
});

router.get('/export-pdf', authAdmin, async (req, res) => {
  const orders = await Order.find()
    .sort({ createdAt: -1 })
    .populate('client', 'firstName lastName')
    .populate('items.article', 'title price');

  const doc = new PDFDocument({ margin: 40 });
  res.setHeader('Content-Type', 'application/pdf');
  res.setHeader('Content-Disposition', 'attachment; filename=commandes.pdf');
  doc.pipe(res);

  doc.fontSize(18).text("Liste des commandes - afk'art", { align: 'center' });
  doc.moveDown();

  orders.forEach((order) => {
    doc.fontSize(12).text(`Commande #${order.orderRef}`);
    doc.fontSize(10).text(
      `Client: ${order.client?.firstName} ${order.client?.lastName} | Statut: ${order.status} | Total: ${order.total} DT`
    );
    doc.moveDown(0.5);
  });

  doc.end();
});

module.exports = router;
```

### Modèle Order (Mongoose)
```js
// models/Order.js
const mongoose = require('mongoose');

const orderSchema = new mongoose.Schema(
  {
    orderRef: { type: String, unique: true },
    client:   { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    items: [
      {
        article: { type: mongoose.Schema.Types.ObjectId, ref: 'Article' },
        quantity: { type: Number, default: 1 },
        price:    { type: Number },
      },
    ],
    deliveryAddress: { type: String },
    paymentMethod:   { type: String, enum: ['carte', 'especes', 'virement'] },
    total: { type: Number },
    status: {
      type: String,
      enum: ['en_attente', 'validee', 'invalidee', 'en cours de livraison'],
      default: 'en_attente',
    },
  },
  { timestamps: true }
);

orderSchema.pre('save', function (next) {
  if (!this.orderRef) {
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const rand = Math.random().toString(36).substring(2, 7).toUpperCase();
    this.orderRef = `CMD-${date}-${rand}`;
  }
  next();
});

module.exports = mongoose.model('Order', orderSchema);
```

### Frontend — Page Admin Commandes (React)
```jsx
// pages/admin/Commandes.jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

const STATUS_COLORS = {
  en_attente: 'badge-warning',
  validee: 'badge-success',
  invalidee: 'badge-danger',
  'en cours de livraison': 'badge-info',
};

export default function AdminCommandes() {
  const [orders, setOrders] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const fetchOrders = async () => {
    const { data } = await axios.get('/admin/commandes', {
      params: { search, status: statusFilter },
    });
    setOrders(data.orders);
  };

  useEffect(() => { fetchOrders(); }, [statusFilter]);

  const handleValidate = async (id) => {
    await axios.patch(`/admin/commandes/${id}/validate`);
    fetchOrders();
  };

  const handleInvalidate = async (id) => {
    await axios.patch(`/admin/commandes/${id}/invalidate`);
    fetchOrders();
  };

  const exportPDF = () => {
    window.open('/admin/commandes/export-pdf', '_blank');
  };

  return <div>...</div>;
}
```

---

## Middleware de protection admin

```js
// middleware/authAdmin.js
const jwt = require('jsonwebtoken');
const User = require('../models/User');

module.exports = async (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ message: 'Non autorise' });

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id);
    if (!user || user.role !== 'admin') {
      return res.status(403).json({ message: 'Acces refuse' });
    }
    req.user = user;
    next();
  } catch {
    res.status(401).json({ message: 'Token invalide' });
  }
};
```

---

## Resume des points cles
- **Articles**: recherche, tri, suppression article/commentaire, stats.
- **Commandes**: filtrage, validation/invalidation, export PDF.
- **Securite**: middleware `authAdmin` sur toutes les routes admin.
- **Frontend React**: etat local, axios, badges de statut, actions conditionnelles.

---

## Regles de role (ADMIN / ARTISANT / CLIENT)

### Role ADMIN — Article
- L'admin a tous les droits sur les articles sans restriction.
- Il peut creer un article meme sans etre artisan.
- Il peut modifier n'importe quel article, sans verification de propriete.
- Il peut supprimer n'importe quel article de la plateforme.
- Il peut consulter les statistiques (par categorie, top artisans) dans le backoffice.

### Difference avec l'artisan
- L'artisan ne peut modifier/supprimer que ses propres articles (`artisan_id == user.id`).
- L'admin bypass cette verification et agit sur tous les articles.

### Role ADMIN — Commande
- L'admin voit toutes les commandes.
- Il peut faire evoluer le statut de `en_attente` vers `confirmee`, puis vers `livree`.
- Il peut annuler une commande tant qu'elle n'est pas livree.
- Il peut assigner un livreur (selon le schema metier/BDD en place).

### Tableau comparatif des roles

| Action | CLIENT | ARTISANT | ADMIN |
|---|---|---|---|
| Voir les articles | ✅ | ✅ | ✅ |
| Creer un article | ❌ | ✅ | ✅ |
| Modifier son article | ❌ | ✅ (sien) | ✅ (tous) |
| Supprimer son article | ❌ | ✅ (sien) | ✅ (tous) |
| Liker un article | ✅ | ❌ | ❌ |
| Commenter un article | ✅ | ❌ | ❌ |
| Repondre aux commentaires | ❌ | ✅ (son article) | ✅ |
| Passer une commande | ✅ | ❌ | ❌ |
| Voir ses commandes | ✅ | ❌ | ✅ (toutes) |
| Confirmer une commande | ❌ | ❌ | ✅ |
| Annuler une commande | ✅ (si en_attente) | ❌ | ✅ |
| Changer statut commande | ❌ | ❌ | ✅ |

---

## Note d'integration dans ce workspace
Ce repository principal est actuellement en **Java/JavaFX**. Ce document sert de base de reference fonctionnelle et technique si vous souhaitez:
1. monter un backoffice web Node/React en parallele,
2. ou porter ces scenarios vers vos services/controllers Java existants.

