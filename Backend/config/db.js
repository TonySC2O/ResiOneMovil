const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    await mongoose.connect("mongodb+srv://admin:admin123@resione.4uufzvb.mongodb.net/ResiOne?retryWrites=true&w=majority");
    console.log('MongoDB conectado correctamente a ResiOne');
  } catch (err) {
    console.log('Error conectando a MongoDB', err);
    process.exit(1);
  }
};

module.exports = connectDB;
